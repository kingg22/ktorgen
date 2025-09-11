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
import io.github.kingg22.ktorgen.core.KtorGen
import io.github.kingg22.ktorgen.core.KtorGenFunction
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
import io.github.kingg22.ktorgen.model.KTOR_CLIENT_PART_DATA
import io.github.kingg22.ktorgen.validator.Validator

class KtorGenProcessor(private val env: SymbolProcessorEnvironment, private val ktorGenOptions: KtorGenOptions) :
    SymbolProcessor {
    companion object {
        lateinit var listType: KSType
        lateinit var arrayType: KSType
        var partDataKtor: KSType? = null
    }
    private val logger = KtorGenLogger(env.logger, ktorGenOptions.errorsLoggingType)
    private val timer = DiagnosticTimer("KtorGen Annotations Processor", logger::logging)
    private var roundCount = 0
    private var firstInvoked = false
        set(value) {
            if (value) timer.start()
            field = value
        }

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        roundCount++
        if (roundCount == 1) {
            firstInvoked = true
            listType = resolver.getKotlinClassByName("kotlin.collections.List")
                ?.asStarProjectedType()
                ?: error("${KtorGenLogger.KTOR_GEN} List not found")
            arrayType = resolver.builtIns.arrayType
            partDataKtor = resolver.getKotlinClassByName(KTOR_CLIENT_PART_DATA)?.asType(emptyList())
            timer.addStep("Retrieve KSTypes")
        }
        var fatalError = false
        val deferredSymbols = mutableListOf<KSAnnotated>()

        try {
            // 1. Todas las funciones anotadas (GET, POST, etc.), agrupadas por clase donde están declaradas
            val annotatedFunctionsGroupedByClass =
                getAnnotatedFunctions(resolver).groupBy { it.closestClassDeclaration() }
            timer.addStep(
                "Retrieve all functions with annotations. Count: ${annotatedFunctionsGroupedByClass.size}",
            )

            // 2. Mapeamos las clases con funciones válidas
            val mapperPhase = timer.createPhase("Extraction and Mapper, round $roundCount")
            mapperPhase.start()

            val classDataWithMethods = annotatedFunctionsGroupedByClass.mapNotNull { (classDec) ->
                if (classDec == null) return@mapNotNull null
                val (classData, symbols) = DeclarationMapper.DEFAULT.mapToModel(classDec) { mapperPhase.createTask(it) }
                if (classData != null) return@mapNotNull classData
                deferredSymbols += symbols
                return@mapNotNull null
            }
            timer.addStep(
                "Process all class data obtained by functions. Count: ${annotatedFunctionsGroupedByClass.size}",
            )

            // 3. También obtenemos todas las clases anotadas con @KtorGen (aunque no tengan métodos)
            val annotatedClasses = getAnnotatedInterfaceTypes(resolver)
            timer.addStep("Retrieve with KtorGen. Count: ${annotatedClasses.count()}")

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
                    if (classData != null) return@mapNotNull classData
                    deferredSymbols += symbols
                    return@mapNotNull null
                }
            timer.addStep(
                "Processed all interfaces and companion with @KtorGen. Count: ${classDataWithMethods.size + classWithoutMethods.count()}",
            )
            mapperPhase.finish()

            // 5. Unimos todas las clases a validar (las que tienen y no tienen funciones)
            val validationPhase = timer.createPhase("Validation for round $roundCount")

            val fullClassList = (classDataWithMethods + classWithoutMethods)
                .distinctBy { it.interfaceName }
                .also { timer.addStep("After filter declarations, have ${it.size}") }
                .filter { it.goingToGenerate }
                .also {
                    timer.addStep("After filter ignored interfaces, have ${it.size} to validate")
                    validationPhase.start()
                }
                .mapNotNull { classData ->
                    Validator.DEFAULT.validate(classData, ktorGenOptions, { validationPhase.createTask(it) }) {
                        fatalError = true
                    }
                }
            timer.addStep(
                "Valid class data ${fullClassList.size}, going to generate all. Have fatal error: $fatalError",
            )
            validationPhase.finish()

            // 6. Generamos el código
            for (classData in fullClassList) {
                KtorGenGenerator.generateKsp(
                    classData,
                    env.codeGenerator,
                    timer.createPhase("Code Generation for ${classData.interfaceName} and round $roundCount"),
                )
            }
            timer.addStep("Generated all classes")

            // deferred errors, util for debug and accumulative errors
            if (fatalError) {
                logger.error(timer.buildErrorsAndWarningsMessage(), null)
            } else if (timer.hasWarnings()) {
                logger.error(timer.buildWarningsMessage(), null)
            }
        } catch (e: Exception) {
            logger.error("${KtorGenLogger.KTOR_GEN} Unexcepted exception caught. \n$e")
        } catch (e: Throwable) {
            if (e.message == null) {
                logger.exception(e)
                logger.error("${KtorGenLogger.KTOR_GEN} Unknown exception caught as Throwable. \n $e")
            } else {
                logger.error(e.message!!, null)
            }
        }
        return deferredSymbols
    }

    override fun finish() {
        super.finish()
        var message: String? = null
        try {
            if (!timer.isFinish()) timer.finish()
            message = timer.buildReport()
        } catch (e: Throwable) {
            logger.info("Failed in diagnostic report with exception: $e", null)
        } finally {
            if (message != null) logger.info(message, null)
        }
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
}
