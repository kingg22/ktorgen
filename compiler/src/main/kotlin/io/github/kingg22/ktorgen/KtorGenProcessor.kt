package io.github.kingg22.ktorgen

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.getKotlinClassByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toAnnotationSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toKModifier
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import io.github.kingg22.ktorgen.core.KtorGen
import io.github.kingg22.ktorgen.core.KtorGenFunction
import io.github.kingg22.ktorgen.core.KtorGenFunctionKmp
import io.github.kingg22.ktorgen.extractor.DeclarationMapper
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
import io.github.kingg22.ktorgen.model.HttpClientClassName
import io.github.kingg22.ktorgen.model.KTOR_CLIENT_PART_DATA
import io.github.kingg22.ktorgen.validator.Validator

class KtorGenProcessor(private val env: SymbolProcessorEnvironment, private val ktorGenOptions: KtorGenOptions) :
    SymbolProcessor {
    companion object {
        lateinit var listType: KSType
        lateinit var arrayType: KSType
        var partDataKtor: KSType? = null
    }
    private val logger = KtorGenLogger(env.logger, ktorGenOptions)
    private val timer = DiagnosticTimer("KtorGen Annotations Processor", logger::logging)
    private var roundCount = 0
    private var firstInvoked = false
        set(value) {
            if (value) timer.start()
            field = value
        }
    private var fatalError = false
    private val deferredSymbols = mutableListOf<Pair<KSClassDeclaration, List<KSAnnotated>>>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        roundCount++
        if (roundCount == 1) {
            firstInvoked = true
            onFirstRound(resolver)
        }

        try {
            val fullClassList = extractAndMapDeclaration(resolver) { ksClassDeclaration, symbols ->
                if (symbols.isNotEmpty()) deferredSymbols += ksClassDeclaration to symbols
            }
            if (!firstInvoked) cleanDeferredSymbolsWith(fullClassList)

            timer.addStep("After filter ignored interfaces, have ${fullClassList.size} to validate")

            if (fullClassList.isEmpty()) {
                timer.addStep("Skipping round $roundCount, no valid class data extracted")
                return finishProcessWithDeferredSymbols()
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
                return finishProcessWithDeferredSymbols()
            }

            timer.addStep(
                "Valid class data ${validClassData.size}, going to generate all. Have fatal error: $fatalError",
            )
            cleanDeferredSymbolsWith(validClassData.toSet())

            // 6. Generamos el código
            for (classData in validClassData) {
                KtorGenGenerator.generateKsp(
                    classData,
                    env.codeGenerator,
                    timer.createPhase("Code Generation for ${classData.interfaceName} and round $roundCount"),
                )
            }

            if (validClassData.isNotEmpty()) timer.addStep("Generated ${validClassData.size} classes")

            // After classes are generated, handle KMP expect function generation
            try {
                generateKmpActualFunctions(validClassData.toSet(), resolver)
            } catch (e: Exception) {
                logger.error("${KtorGenLogger.KTOR_GEN} Error generating KMP actual functions.", null)
                logger.exception(e)
            }
        } catch (e: Exception) {
            logger.fatalError("${KtorGenLogger.KTOR_GEN} Unexcepted exception caught. \n$e")
            logger.exception(e)
        } catch (e: Throwable) {
            if (e.message == null) {
                logger.error("${KtorGenLogger.KTOR_GEN} Unknown exception caught as Throwable. \n $e")
            } else {
                logger.error(e.message!!, null)
            }
            logger.exception(e)
        }

        return finishProcessWithDeferredSymbols()
    }

    override fun finish() {
        var message: String? = null

        try {
            // deferred errors, util for debug and accumulative errors
            if (fatalError) {
                logger.fatalError(timer.buildErrorsAndWarningsMessage(), null)
            } else if (timer.hasWarnings()) {
                logger.error(timer.buildWarningsMessage(), null)
            }

            if (deferredSymbols.isNotEmpty()) {
                logger.fatalError(
                    "${KtorGenLogger.KTOR_GEN} " + deferredSymbolsMessage("finish round"),
                    deferredSymbols.first().first,
                )
            }

            if (!timer.isFinish()) timer.finish()
            message = timer.buildReport()
        } catch (e: Throwable) {
            logger.warn("Failed in diagnostic report with exception.", null)
            logger.exception(e)
        } finally {
            if (message != null) logger.info(message, null)
            super.finish()
        }
    }

    /** Print a step message if deferred symbols is not empty in the current round, and return symbols */
    @Suppress("NOTHING_TO_INLINE")
    private inline fun finishProcessWithDeferredSymbols(): List<KSAnnotated> {
        if (deferredSymbols.isNotEmpty()) timer.addStep(deferredSymbolsMessage("round $roundCount"))
        return deferredSymbols.flatMap { it.second }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun deferredSymbolsMessage(round: String) =
        "Found ${deferredSymbols.size} unresolved symbols on $round: " +
            deferredSymbols.joinToString(prefix = "[", postfix = "]") {
                it.first.simpleName.asString() + " => " +
                    it.second.joinToString(prefix = "[", postfix = "]") { s -> s.toString() }
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
                    else -> {
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

    @OptIn(KspExperimental::class)
    private fun onFirstRound(resolver: Resolver) {
        listType = resolver.getKotlinClassByName("kotlin.collections.List")
            ?.asStarProjectedType()
            ?: error("${KtorGenLogger.KTOR_GEN} List not found")
        arrayType = resolver.builtIns.arrayType
        partDataKtor = resolver.getKotlinClassByName(KTOR_CLIENT_PART_DATA)?.asType(emptyList())
        timer.addStep("Retrieve KSTypes")
    }

    private fun cleanDeferredSymbolsWith(fullClassList: Set<ClassData>) {
        val interfaceNames = fullClassList.mapNotNull { it.ksClassDeclaration.qualifiedName?.asString() }.toSet()
        val allRelatedNames = fullClassList.flatMap { classData ->
            classData.superClasses.mapNotNull {
                val type = it.resolve()
                if (type.isError) return@mapNotNull null
                type.declaration.qualifiedName?.asString()
            }
        } + interfaceNames

        deferredSymbols.removeAll { (decl, _) ->
            allRelatedNames.contains(decl.qualifiedName?.asString())
        }
    }

    private fun extractAndMapDeclaration(
        resolver: Resolver,
        onDeferredSymbols: (KSClassDeclaration, List<KSAnnotated>) -> Unit,
    ): Set<ClassData> {
        // 1. Todas las funciones anotadas (GET, POST, etc.), agrupadas por clase donde están declaradas
        val annotatedFunctionsGroupedByClass = getAnnotatedFunctions(resolver)
            .groupBy { it.closestClassDeclaration() }

        timer.addStep(
            "Retrieve all functions with annotations. Count: ${annotatedFunctionsGroupedByClass.size}",
        )

        // 2. Mapeamos las clases con funciones válidas
        val mapperPhase = timer.createPhase("Extraction and Mapper, round $roundCount")
        mapperPhase.start()

        val classDataWithMethods = annotatedFunctionsGroupedByClass.mapNotNull { (classDec) ->
            if (classDec == null) return@mapNotNull null
            val (classData, symbols) = DeclarationMapper.DEFAULT.mapToModel(classDec) { mapperPhase.createTask(it) }
            onDeferredSymbols(classDec, symbols)
            classData
        }

        timer.addStep(
            "Process all class data obtained by functions. Count: ${annotatedFunctionsGroupedByClass.size}",
        )

        // 3. También obtenemos todas las clases anotadas con @KtorGen (aunque no tengan métodos)
        val annotatedClasses = getAnnotatedInterfaceTypes(resolver).toList()
        timer.addStep("Retrieve with KtorGen. Count: ${annotatedClasses.size}")

        // 4. Filtramos aquellas clases que no están en `groupedByClass` → no tienen funciones válidas
        val classWithoutMethods = annotatedClasses
            .filter { it !in annotatedFunctionsGroupedByClass.keys }
            .also {
                timer.addStep(
                    "After sum (functions + ktorGen interfaces (or its companion)) - already extracted, count: ${it.count()}",
                )
            }
            .mapNotNull { classDeclaration ->
                val (classData, symbols) = DeclarationMapper.DEFAULT.mapToModel(classDeclaration) {
                    mapperPhase.createTask(it)
                }
                onDeferredSymbols(classDeclaration, symbols)
                classData
            }.toList()

        timer.addStep(
            "Processed all interfaces and companion with @KtorGen. Count: ${classDataWithMethods.size + classWithoutMethods.size}",
        )
        mapperPhase.finish()

        // 5. Unimos todas las clases a validar (las que tienen y no tienen funciones)
        return (classDataWithMethods + classWithoutMethods)
            .distinctBy { it.interfaceName }
            .also { timer.addStep("After filter declarations, have ${it.size}") }
            .filter { it.goingToGenerate }
            .toSet()
    }

    // FIXME, move to processor pipeline and attach to corresponding class data
    @OptIn(io.github.kingg22.ktorgen.core.KtorGenExperimental::class)
    private fun generateKmpActualFunctions(classDataSet: Set<ClassData>, resolver: Resolver) {
        val functions = resolver.getSymbolsWithAnnotation(KtorGenFunctionKmp::class.qualifiedName!!)
            .filterIsInstance<KSFunctionDeclaration>()
            .toList()

        if (functions.isEmpty()) return

        timer.addStep("Found ${functions.size} functions annotated with @KtorGenFunctionKmp")

        val classDataByQName = classDataSet.associateBy { it.ksClassDeclaration.qualifiedName?.asString() }

        functions.forEach { func ->
            val name = func.simpleName.asString()
            val pkg = func.packageName.asString()

            // must be expect function
            if (!func.modifiers.contains(Modifier.EXPECT)) {
                logger.error(
                    "A function annotated with @KtorGenFunctionKmp must be 'expect' in common source set",
                    func,
                )
                return@forEach
            }

            val returnType = func.returnType?.resolve()
            if (returnType == null || returnType.isError) {
                // unresolved, wait for the next round
                timer.addStep("Return type for $name is not resolved yet, will try next round")
                return@forEach
            }
            val returnDecl = returnType.declaration as? KSClassDeclaration
            val returnQName = returnDecl?.qualifiedName?.asString()
            if (returnDecl == null || returnQName == null) {
                logger.error("@KtorGenFunctionKmp function must return an interface annotated with @KtorGen", func)
                return@forEach
            }

            val classData = classDataByQName[returnQName]
            if (classData == null) {
                logger.error(
                    "A function annotated with @KtorGenFunctionKmp needs to return a generated interface (@KtorGen annotated). Not found for $returnQName",
                    func,
                )
                return@forEach
            }

            // compute the expected constructor parameter count: always HttpClient + interface properties (excluding HttpClient) + super interfaces
            val httpClientTypeName = HttpClientClassName
            val constructorTypes = buildList {
                add(httpClientTypeName)
                // properties excluding HttpClient
                classData.properties
                    .map { it.type.toTypeName() }
                    .filter { it != httpClientTypeName }
                    .forEach { add(it) }
                // one param per super interface
                classData.superClasses.forEach { ref ->
                    add(ref.resolve().toClassName())
                }
            }

            val expectParamTypes = func.parameters.map { it.type.resolve().toTypeName() }

            if (constructorTypes.size != expectParamTypes.size) {
                logger.error(
                    "Constructor mismatch for ${classData.generatedName}. Expect function '$name' has ${expectParamTypes.size} parameter(s) but generated constructor expects ${constructorTypes.size} parameter(s)",
                    func,
                )
                return@forEach
            }

            // Generate actual function
            val implClassName = ClassName(classData.packageNameString, classData.generatedName)
            val returnClassName = ClassName(classData.packageNameString, classData.interfaceName)

            val funBuilder = FunSpec.builder(name)
                .returns(returnClassName)
                .addModifiers(func.modifiers.mapNotNull { it.toKModifier() }.filter { it != KModifier.EXPECT }.toSet())
                .addModifiers(KModifier.ACTUAL)
                .addOriginatingKSFile(func.containingFile!!)
                .addAnnotations(func.annotations.map { it.toAnnotationSpec(true) }.toList())

            // copy parameters
            func.parameters.forEach { p ->
                val pName = p.name?.asString() ?: "p"
                funBuilder.addParameter(
                    ParameterSpec.builder(pName, p.type.resolve().toTypeName()).build(),
                )
            }

            val argsJoin = func.parameters.joinToString { it.name?.asString() ?: "p" }
            funBuilder.addStatement("return %T(%L)", implClassName, argsJoin)

            // build file and write
            val file = FileSpec.builder(pkg, "_${name}KmpActual")
                .addFunction(funBuilder.build())
                .addAnnotation(
                    AnnotationSpec.builder(Suppress::class) // suppress annotations
                        .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                        .addMember("%S", "REDUNDANT_VISIBILITY_MODIFIER")
                        .addMember("%S", "unused")
                        .addMember("%S", "UNUSED_IMPORT")
                        .addMember("%S", "warnings")
                        .addMember("%S", "RemoveSingleExpressionStringTemplate")
                        .addMember("%S", "ktlint")
                        .addMember("%S", "detekt:all")
                        .build(),
                )
                .build()

            file.writeTo(env.codeGenerator, aggregating = false)
            timer.addStep("Generated actual function for $name in package $pkg")
        }
    }
}
