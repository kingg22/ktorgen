package io.github.kingg22.ktorgen

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.getKotlinClassByName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ksp.writeTo
import io.github.kingg22.ktorgen.KtorGenOptions.ErrorsLoggingType.Errors
import io.github.kingg22.ktorgen.core.KtorGen
import io.github.kingg22.ktorgen.core.KtorGenFunction
import io.github.kingg22.ktorgen.core.KtorGenFunctionKmp
import io.github.kingg22.ktorgen.extractor.DeclarationMapper
import io.github.kingg22.ktorgen.generator.KotlinpoetGenerator
import io.github.kingg22.ktorgen.generator.KtorGenGenerator
import io.github.kingg22.ktorgen.http.DELETE
import io.github.kingg22.ktorgen.http.GET
import io.github.kingg22.ktorgen.http.HEAD
import io.github.kingg22.ktorgen.http.HTTP
import io.github.kingg22.ktorgen.http.OPTIONS
import io.github.kingg22.ktorgen.http.PATCH
import io.github.kingg22.ktorgen.http.POST
import io.github.kingg22.ktorgen.http.PUT
import io.github.kingg22.ktorgen.model.ClassData
import io.github.kingg22.ktorgen.model.KTOR_CLIENT_PART_DATA
import io.github.kingg22.ktorgen.validator.Validator

internal class KtorGenProcessor(
    private val env: SymbolProcessorEnvironment,
    private val logger: KtorGenLogger,
    private val ktorGenOptions: KtorGenOptions,
) : SymbolProcessor {
    private val timer = DiagnosticTimer("KtorGen Annotations Processor", logger::logging)
    private var roundCount = 0
    private var fatalError = false
    private val deferredSymbols = mutableListOf<DeferredSymbolRef>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        roundCount++
        if (roundCount == 1) onFirstRound()

        try {
            val (fullClassList, kmpExpectFunctions) = extractAndMapDeclaration(
                resolver,
                onDeferredSymbols = { ksClassDeclaration, symbols ->
                    if (symbols.isEmpty()) return@extractAndMapDeclaration

                    val ownerFqName = ksClassDeclaration
                        .qualifiedName
                        ?.asString()
                        ?: return@extractAndMapDeclaration

                    val unresolvedFqNames = symbols.mapNotNull { symbol ->
                        when (symbol) {
                            is KSDeclaration -> symbol.qualifiedName?.asString()
                            else -> null
                        }
                    }

                    if (unresolvedFqNames.isNotEmpty()) {
                        deferredSymbols += DeferredSymbolRef(
                            ownerClassFqName = ownerFqName,
                            unresolvedSymbolFqNames = unresolvedFqNames,
                        )
                    }
                },
            )
            if (roundCount > 1) cleanDeferredSymbolsWith(fullClassList)

            timer.addStep("After filter ignored interfaces, have ${fullClassList.size} to validate")

            if (fullClassList.isEmpty()) {
                timer.addStep("Skipping round $roundCount, no valid class data extracted")
                return finishProcessWithDeferredSymbols(resolver)
            }

            val validationPhase = timer.createPhase("Validation for round $roundCount")
            validationPhase.start()

            val validClassData = fullClassList.mapNotNull { classData ->
                Validator.DEFAULT.validate(classData, ktorGenOptions, { validationPhase.createTask(it) }) {
                    fatalError = true
                }
            }

            validationPhase.finish()

            if (validClassData.isEmpty()) {
                timer.addStep("Skipping round $roundCount, no valid class data found")
                return finishProcessWithDeferredSymbols(resolver)
            }

            timer.addStep(
                "Valid class data ${validClassData.size}, going to generate all. Have fatal error: $fatalError",
            )
            cleanDeferredSymbolsWith(validClassData.toSet())

            // 6. Generamos el código
            for (classData in validClassData) {
                context(timer.createPhase("Code Generation for ${classData.interfaceName} and round $roundCount")) {
                    generateKsp(classData, env.codeGenerator, resolver)
                }
            }

            if (validClassData.isNotEmpty()) timer.addStep("Generated ${validClassData.size} classes")

            if (kmpExpectFunctions.isNotEmpty()) {
                timer.addStep("Unresolved ${kmpExpectFunctions.size} @KtorGenFunctionKmp", kmpExpectFunctions.first())
            }
        } catch (e: KtorGenFatalError) {
            logger.fatalError("${KtorGenLogger.KTOR_GEN} ${e.message}")
            logger.exception(e)
            fatalError = true
        } catch (e: Exception) {
            logger.fatalError("${KtorGenLogger.KTOR_GEN} Unexpected exception caught. \n$e")
            logger.exception(e)
        }

        return finishProcessWithDeferredSymbols(resolver)
    }

    override fun finish() {
        var message: String? = null

        try {
            if (fatalError) {
                val msg = if (ktorGenOptions.errorsLoggingType == Errors) {
                    timer.buildErrorsAndWarningsMessage()
                } else {
                    timer.buildErrorsMessage()
                }
                logger.fatalError(msg)
            } else if (timer.hasWarnings()) {
                logger.error(timer.buildWarningsMessage(), null)
            }

            if (deferredSymbols.isNotEmpty()) {
                logger.fatalError(
                    "${KtorGenLogger.KTOR_GEN} " +
                        deferredSymbolsMessage("finish round"),
                )
            }

            if (!timer.isFinish()) timer.finish()
            message = timer.buildReport()
        } catch (e: Exception) {
            logger.warn("Failed in diagnostic report with exception.", null)
            logger.exception(e)
        } finally {
            if (message != null) logger.info(message, null)
            super.finish()
        }
    }

    /** Generate the Impl class using [KotlinpoetGenerator] of ksp */
    context(_: DiagnosticSender)
    private fun generateKsp(classData: ClassData, codeGenerator: CodeGenerator, resolver: Resolver) {
        val (partDataKtor, listType, arrayType) = resolveTypes(resolver)
        KtorGenGenerator.DEFAULT.generate(classData, partDataKtor, listType, arrayType).forEach { fileSpec ->
            fileSpec.writeTo(codeGenerator, false)
        }
    }

    /** @return PartData of Ktor Client, Kotlin List, and Array types */
    @OptIn(KspExperimental::class)
    context(timer: DiagnosticSender)
    private fun resolveTypes(resolver: Resolver): Triple<KSType?, KSType, KSType> {
        val listType = timer.requireNotNull(
            resolver.getKotlinClassByName("kotlin.collections.List")?.asStarProjectedType(),
            "Kotlin List type not found",
        )
        val arrayType = resolver.builtIns.arrayType
        val partDataKtor = resolver.getKotlinClassByName(KTOR_CLIENT_PART_DATA)?.asType(emptyList())

        // Only log in the first round, skip repeat the same logs in all rounds
        timer.addStep("Retrieve KSTypes [Ktor PartData class founded: ${partDataKtor != null}]")
        return Triple(partDataKtor, listType, arrayType)
    }

    /** Print a step message if deferred symbols is not empty in the current round, and return symbols */
    private fun finishProcessWithDeferredSymbols(resolver: Resolver): List<KSAnnotated> {
        val unresolved = mutableListOf<KSAnnotated>()

        for (ref in deferredSymbols) {
            for (fqName in ref.unresolvedSymbolFqNames) {
                resolver.getClassDeclarationByName(
                    resolver.getKSNameFromString(fqName),
                )?.let(unresolved::add)
            }
        }

        if (unresolved.isNotEmpty()) {
            timer.addStep(deferredSymbolsMessage("round $roundCount"))
        }

        return unresolved
    }

    private fun deferredSymbolsMessage(round: String): String = buildString {
        append("Found ${deferredSymbols.size} unresolved symbols on $round:\n[")

        deferredSymbols.joinTo(
            this,
            separator = "\n",
        ) { ref ->
            buildString {
                append(ref.ownerClassFqName)
                append(" => ")
                append(ref.unresolvedSymbolFqNames.joinToString(prefix = "[", postfix = "]"))
            }
        }

        append("\n]")
    }

    private fun getAnnotatedInterfaceTypes(resolver: Resolver) =
        resolver.getSymbolsWithAnnotation(KtorGen::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .mapNotNull { decl ->
                when (decl.classKind) {
                    ClassKind.INTERFACE -> decl

                    ClassKind.OBJECT, ClassKind.CLASS -> if (decl.isCompanionObject) {
                        decl.parentDeclaration as? KSClassDeclaration
                    } else {
                        logger.error(KtorGenLogger.KTOR_GEN_TYPE_NOT_ALLOWED, decl)
                        null
                    }

                    ClassKind.ENUM_CLASS, ClassKind.ENUM_ENTRY, ClassKind.ANNOTATION_CLASS -> {
                        logger.error(KtorGenLogger.KTOR_GEN_TYPE_NOT_ALLOWED, decl)
                        null
                    }
                }
            }
            .filter { it.classKind == ClassKind.INTERFACE }

    private fun getAnnotatedFunctions(resolver: Resolver): Sequence<KSFunctionDeclaration> {
        val getAnnotated = resolver.getSymbolsWithAnnotation(GET::class.qualifiedName!!)
        val postAnnotated = resolver.getSymbolsWithAnnotation(POST::class.qualifiedName!!)
        val putAnnotated = resolver.getSymbolsWithAnnotation(PUT::class.qualifiedName!!)
        val deleteAnnotated = resolver.getSymbolsWithAnnotation(DELETE::class.qualifiedName!!)
        val headAnnotated = resolver.getSymbolsWithAnnotation(HEAD::class.qualifiedName!!)
        val optionsAnnotated = resolver.getSymbolsWithAnnotation(OPTIONS::class.qualifiedName!!)
        val patchAnnotated = resolver.getSymbolsWithAnnotation(PATCH::class.qualifiedName!!)
        val httpAnnotated = resolver.getSymbolsWithAnnotation(HTTP::class.qualifiedName!!)
        val genAnnotated = resolver.getSymbolsWithAnnotation(KtorGenFunction::class.qualifiedName!!)

        return (
            getAnnotated +
                postAnnotated +
                putAnnotated +
                deleteAnnotated +
                headAnnotated +
                optionsAnnotated +
                patchAnnotated +
                httpAnnotated +
                genAnnotated
            ).filterIsInstance<KSFunctionDeclaration>().distinct()
    }

    private fun onFirstRound() {
        timer.start()
    }

    private fun cleanDeferredSymbolsWith(validClasses: Set<ClassData>) {
        val resolvedClassNames = validClasses.map { it.qualifiedName }.toSet()

        deferredSymbols.removeAll { ref ->
            ref.ownerClassFqName in resolvedClassNames
        }
    }

    private fun extractAndMapDeclaration(
        resolver: Resolver,
        onDeferredSymbols: (KSClassDeclaration, List<KSAnnotated>) -> Unit,
    ): ClassDataWithKmpExpectFunctions {
        // 1. Todas las funciones anotadas (GET, POST, etc.), agrupadas por clase donde están declaradas
        val mapperPhase = timer.createPhase("Extraction and Mapper, round $roundCount")
        mapperPhase.start()

        val annotatedFunctionsGroupedByClass = getAnnotatedFunctions(resolver)
            .groupBy { it.closestClassDeclaration() }

        mapperPhase.addStep(
            "Retrieve all functions with annotations. Count: ${annotatedFunctionsGroupedByClass.size}",
        )

        val kmpExpectFunctions = resolver.getSymbolsWithAnnotation(KtorGenFunctionKmp::class.qualifiedName!!)
            .filterIsInstance<KSFunctionDeclaration>()
            .toMutableList()

        mapperPhase.addStep("Retrieve all KtorGenFunctionKmp. Count: ${kmpExpectFunctions.size}")

        fun declarationMap(classDec: KSClassDeclaration): ClassData? {
            val (matching, remaining) = kmpExpectFunctions.partition { function ->
                val returnTypeFunction = function.returnType?.resolve() ?: return@partition false
                if (returnTypeFunction.isError) return@partition false

                val returnDecl = returnTypeFunction.declaration as? KSClassDeclaration
                val returnQName = returnDecl?.qualifiedName?.asString()

                if (returnDecl == null || returnQName == null) {
                    mapperPhase.addError(
                        "@KtorGenFunctionKmp function must return an interface annotated with @KtorGen",
                        function,
                    )
                }

                returnQName == classDec.qualifiedName?.asString()
            }

            kmpExpectFunctions.clear()
            kmpExpectFunctions.addAll(remaining)

            val (classData, symbols) = context(
                mapperPhase.createTask(DeclarationMapper.DEFAULT.getLoggerNameFor(classDec)),
            ) {
                DeclarationMapper.DEFAULT.mapToModel(classDec, matching)
            }

            onDeferredSymbols(classDec, symbols)
            return classData
        }

        // 2. Mapeamos las clases con funciones válidas
        val classDataWithMethods = annotatedFunctionsGroupedByClass.mapNotNull { (classDec) ->
            classDec?.let { declarationMap(it) }
        }

        mapperPhase.addStep(
            "Process all class data obtained by functions. Count: ${annotatedFunctionsGroupedByClass.size}",
        )

        // 3. También obtenemos todas las clases anotadas con @KtorGen (aunque no tengan métodos)
        val annotatedClasses = getAnnotatedInterfaceTypes(resolver).toList()
        mapperPhase.addStep("Retrieve with @KtorGen count: ${annotatedClasses.size}")

        // 4. Filtramos aquellas clases que no están en `groupedByClass` → no tienen funciones válidas
        val classWithoutMethods = annotatedClasses
            .filter { it !in annotatedFunctionsGroupedByClass.keys }
            .also {
                mapperPhase.addStep(
                    "After sum (functions + ktorGen interfaces (or its companion)) - already extracted, count: ${it.count()}",
                )
            }
            .mapNotNull { declarationMap(it) }
            .toList()

        mapperPhase.addStep(
            "Processed all interfaces and companion with @KtorGen. Count: ${classDataWithMethods.size + classWithoutMethods.size}",
        )

        // 5. Unimos todas las clases a validar (las que tienen y no tienen funciones)
        val classDataSet = (classDataWithMethods + classWithoutMethods)
            .distinctBy { it.interfaceName }
            .also { mapperPhase.addStep("After filter declarations, have ${it.size}") }
            .filter { it.goingToGenerate }
            .toSet()
        mapperPhase.addStep("Finally goingToGenerate ${classDataSet.size}")

        if (classDataSet.isEmpty() && kmpExpectFunctions.isNotEmpty()) {
            mapperPhase.addWarning(
                "No valid class data extracted, but found @KtorGenFunctionKmp, can be a bug in the processor. " +
                    "Found ${kmpExpectFunctions.size} annotated functions.",
                kmpExpectFunctions.first(),
            )
        }

        mapperPhase.finish()

        return classDataSet to kmpExpectFunctions
    }

    private typealias ClassDataWithKmpExpectFunctions = Pair<Set<ClassData>, List<KSFunctionDeclaration>>
    private data class DeferredSymbolRef(val ownerClassFqName: String, val unresolvedSymbolFqNames: List<String>)
}
