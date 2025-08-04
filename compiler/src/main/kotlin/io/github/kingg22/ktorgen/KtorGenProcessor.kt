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
import io.github.kingg22.ktorgen.validator.Validator

class KtorGenProcessor(private val env: SymbolProcessorEnvironment, private val ktorGenOptions: KtorGenOptions) :
    SymbolProcessor {
    companion object {
        lateinit var listType: KSType
        lateinit var arrayType: KSType
    }
    private val logger = KtorGenLogger(env.logger, ktorGenOptions.errorsLoggingType)
    private var invoked = false

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) return emptyList()
        invoked = true

        val timer = Timer("KtorGen Annotations Processor")
        try {
            timer.start()

            listType = resolver.getKotlinClassByName("kotlin.collections.List")
                ?.asStarProjectedType()
                ?: error("${KtorGenLogger.KTOR_GEN} List not found")
            arrayType = resolver.builtIns.arrayType
            timer.markStepCompleted("Retrieve KSTypes")

            // 1. Todas las funciones anotadas (GET, POST, etc.), agrupadas por clase donde están declaradas
            val annotatedFunctionsGroupedByClass =
                getAnnotatedFunctions(resolver).groupBy { it.closestClassDeclaration() }
            timer.markStepCompleted(
                "Retrieve all functions with annotations. Count: ${annotatedFunctionsGroupedByClass.size}",
            )

            // 2. Mapeamos las clases con funciones válidas
            val classDataWithMethods = annotatedFunctionsGroupedByClass.mapNotNull { (classDec) ->
                if (classDec == null) {
                    null
                } else {
                    DeclarationMapper.DEFAULT.mapToModel(classDec).also {
                        timer.markStepCompleted("Mapped $it")
                    }
                }
            }
            timer.markStepCompleted(
                "Process all class data obtained by functions. Count: ${annotatedFunctionsGroupedByClass.size}",
            )

            // 3. También obtenemos todas las clases anotadas con @KtorGen (aunque no tengan métodos)
            val annotatedClasses = getAnnotatedInterfaceTypes(resolver)
            timer.markStepCompleted("Retrieve with KtorGen. Detail: $annotatedClasses")

            // 4. Filtramos aquellas clases que no están en `groupedByClass` → no tienen funciones válidas
            val classWithoutMethods = annotatedClasses
                .filter { it !in annotatedFunctionsGroupedByClass.keys }
                .also {
                    timer.markStepCompleted("After sum functions + ktorGen types, count of ktorGen is: $it")
                }
                .map { DeclarationMapper.DEFAULT.mapToModel(it) }
            timer.markStepCompleted(
                "Process all interfaces and companion with @KtorGen. Detail: $classWithoutMethods.\n" +
                    "Sum of both: ${classDataWithMethods + classWithoutMethods}",
            )

            // 5. Unimos todas las clases a validar (las que tienen y no tienen funciones)
            val fullClassList = (classDataWithMethods + classWithoutMethods)
                .distinctBy { it.interfaceName }
                .also { timer.markStepCompleted("After filter declarations, have ${it.size} to validate") }
                .mapNotNull {
                    Validator.validate(it, ktorGenOptions, logger)
                }
            timer.markStepCompleted("Valid class data ${fullClassList.size}, going to generate all")

            // 6. Generamos el código
            for (classData in fullClassList) {
                KtorGenGenerator.generateKsp(classData, env.codeGenerator)
            }
            timer.markStepCompleted("Generate all classes")
        } catch (e: Throwable) {
            logger.exception(
                IllegalStateException(
                    "${KtorGenLogger.KTOR_GEN} Unexcepted exception caught. \n$e",
                    e,
                ),
            )
            throw e
        } finally {
            timer.finishAndPrint(logger)
        }

        return emptyList()
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
