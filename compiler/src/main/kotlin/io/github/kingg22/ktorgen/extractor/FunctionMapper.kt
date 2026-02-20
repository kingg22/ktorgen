package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ksp.toKModifier
import io.github.kingg22.ktorgen.DiagnosticSender
import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.checkImplementation
import io.github.kingg22.ktorgen.core.KtorGenFunction
import io.github.kingg22.ktorgen.http.Cookie
import io.github.kingg22.ktorgen.http.FormUrlEncoded
import io.github.kingg22.ktorgen.http.Fragment
import io.github.kingg22.ktorgen.http.HTTP
import io.github.kingg22.ktorgen.http.Header
import io.github.kingg22.ktorgen.http.Multipart
import io.github.kingg22.ktorgen.model.FunctionData
import io.github.kingg22.ktorgen.model.KTORGEN_DEFAULT_VALUE
import io.github.kingg22.ktorgen.model.TypeData
import io.github.kingg22.ktorgen.model.annotations.FunctionAnnotation
import io.github.kingg22.ktorgen.model.annotations.HttpMethod
import io.github.kingg22.ktorgen.model.options.FunctionGenerationOptions
import io.github.kingg22.ktorgen.require
import io.github.kingg22.ktorgen.requireNotNull
import io.github.kingg22.ktorgen.work

internal class FunctionMapper : DeclarationFunctionMapper {
    context(timer: DiagnosticSender)
    override fun mapToModel(
        declaration: KSFunctionDeclaration,
        basePath: String,
    ): DeclarationFunctionMapper.FunctionDataOrDeferredSymbols {
        val name = declaration.simpleName.asString()
        val deferredSymbols = mutableListOf<KSAnnotated>()
        return timer.work {
            checkImplementation(!declaration.modifiers.contains(Modifier.EXPECT)) {
                "Expect functions are not supported in this mapper. Declaration: $declaration"
            }
            timer.require(
                !declaration.modifiers.contains(Modifier.EXTERNAL),
                KtorGenLogger.EXTERNAL_DECLARATION_NOT_ALLOWED,
                declaration,
            )
            timer.addStep("Extracting the KtorGenFunction annotation")
            val options = extractKtorGenFunction(declaration) ?: FunctionGenerationOptions.DEFAULT

            if (!options.goingToGenerate) {
                timer.require(!declaration.isAbstract, KtorGenLogger.ABSTRACT_FUNCTION_IGNORED, declaration)
                timer.addStep("Skipping, not going to generate this function.")
                return@work null to emptyList()
            }

            val type = timer.requireNotNull(
                declaration.returnType?.resolve(),
                KtorGenLogger.FUNCTION_NOT_RETURN_TYPE + name,
                declaration,
            )
            if (type.isError) {
                deferredSymbols += type.declaration
                return@work null to deferredSymbols
            }

            val returnType = TypeData(type)
            timer.addStep("Processed return type: ${returnType.typeName}")

            val (annotationsOptions, symbols) = declaration.extractAnnotationOptions()
            deferredSymbols += symbols

            val functionAnnotations = context(timer.createTask("Extract annotations")) {
                getFunctionAnnotations(declaration, basePath)
            }

            timer.addStep("Processed function annotations")

            val parameters = declaration.parameters.mapNotNull { param ->
                val (parameterData, symbols) = context(
                    timer.createTask(DeclarationParameterMapper.DEFAULT.getLoggerNameFor(param)),
                ) {
                    DeclarationParameterMapper.DEFAULT.mapToModel(param)
                }
                parameterData?.let {
                    timer.addStep("Processed param: ${it.nameString}")
                    return@mapNotNull parameterData
                }
                deferredSymbols += symbols
                return@mapNotNull null
            }

            timer.addStep("Processed parameters")

            val isSuspend = declaration.modifiers.contains(Modifier.SUSPEND)
            val modifiers = declaration.modifiers.mapNotNull { it.toKModifier() } + KModifier.OVERRIDE

            if (deferredSymbols.isNotEmpty()) {
                timer.addStep("Found deferred symbols, skipping to next round of processing")
                return@work null to deferredSymbols
            }

            timer.addStep("Finishing mapping")

            FunctionData(
                name = name,
                returnTypeData = returnType,
                isSuspend = isSuspend,
                parameterDataList = parameters.asSequence(),
                ktorGenAnnotations = functionAnnotations.asSequence(),
                httpMethodAnnotation =
                functionAnnotations.filterIsInstance<FunctionAnnotation.HttpMethodAnnotation>().first(),
                modifierSet = modifiers.filterNot {
                    /* In java methods of interfaces are marked with abstract, in kotlin KSP don't mark as abstract instead use declaration.isAbstract */
                    it == KModifier.ABSTRACT
                }.toSet(),
                ksFunctionDeclaration = declaration,
                options = options,
                annotationsOptions = annotationsOptions,
            ) to emptyList()
        }
    }

    context(timer: DiagnosticSender)
    private fun getFunctionAnnotations(function: KSFunctionDeclaration, basePath: String) = timer.work {
        buildList {
            timer.addStep("Start collect KtorGen annotations, first Http Method")

            val method = httpAnnotationResolver(function, basePath)
            if (method.isEmpty()) {
                add(FunctionAnnotation.HttpMethodAnnotation(basePath, HttpMethod.Absent))
                timer.addStep("Http method not found, adding absent value, need validation!", function)
            } else {
                timer.require(method.size == 1, function) {
                    "${KtorGenLogger.ONLY_ONE_HTTP_METHOD_IS_ALLOWED} Found: " +
                        method.joinToString(prefix = "[", postfix = "]") { it.httpMethod.value }
                }
                val http = method.first()
                add(http)
                timer.addStep("Processed http annotation $http")
            }

            timer.addStep("Going to get Fragment")
            function.getAnnotation(manualExtraction = {
                FunctionAnnotation.Fragment(
                    it.getArgumentValueByName<String>("value").replaceExact(KTORGEN_DEFAULT_VALUE, ""),
                    it.getArgumentValueByName("encoded") ?: false,
                )
            }) { fragment: Fragment ->
                FunctionAnnotation.Fragment(fragment.value.replaceExact(KTORGEN_DEFAULT_VALUE, ""), fragment.encoded)
            }?.also {
                add(it)
                timer.addStep("Fragment found")
            }

            timer.addStep("Going to get Multipart")
            if (function.isAnnotationPresent<Multipart>()) {
                add(FunctionAnnotation.Multipart)
                timer.addStep("Multipart found")
            }

            timer.addStep("Going to get FormUrlEncoded")
            if (function.isAnnotationPresent<FormUrlEncoded>()) {
                add(FunctionAnnotation.FormUrlEncoded)
                timer.addStep("FormUrlEncoded found")
            }

            timer.addStep("Going to get Headers")
            function.getAnnotationsByType<Header>()
                .map { it.name to it.value }
                .toList()
                .takeIf { it.isNotEmpty() }
                ?.let { headers ->
                    timer.addStep("Header found")
                    add(FunctionAnnotation.Headers(headers))
                }

            timer.addStep("Going to get Cookies")
            function.getAnnotationsByType<Cookie>()
                .map { it.toCookieValues() }
                .toList()
                .takeIf { it.isNotEmpty() }
                ?.let { cookies ->
                    timer.addStep("Cookies found")
                    add(FunctionAnnotation.Cookies(cookies))
                }
        }
    }

    private fun httpAnnotationResolver(
        function: KSFunctionDeclaration,
        basePath: String,
    ): List<FunctionAnnotation.HttpMethodAnnotation> {
        @OptIn(KspExperimental::class)
        fun KSFunctionDeclaration.parseHTTPMethod(name: String): FunctionAnnotation.HttpMethodAnnotation? {
            val annotation = this.annotations.firstOrNull { it.shortName.asString() == name }
            return if (annotation == null) {
                null
            } else if (name == "HTTP") {
                this.getAnnotation<HTTP, FunctionAnnotation.HttpMethodAnnotation>(
                    manualExtraction = {
                        FunctionAnnotation.HttpMethodAnnotation(
                            basePath + it.getArgumentValueByName<String>("path").orEmpty(),
                            HttpMethod.parse(it.getArgumentValueByName<String>("method")!!.uppercase()),
                        )
                    },
                    mapFromAnnotation = {
                        FunctionAnnotation.HttpMethodAnnotation(
                            basePath + it.path,
                            HttpMethod.parse(it.method.uppercase()),
                        )
                    },
                )
            } else {
                val value = when (val path = annotation.getArgumentValueByName<Any?>("value")) {
                    is String -> path
                    is ArrayList<*> -> path.firstOrNull() as? String
                    else -> null
                }
                FunctionAnnotation.HttpMethodAnnotation(
                    (basePath + value.orEmpty()),
                    HttpMethod.parse(name.uppercase()),
                )
            }
        }

        return listOfNotNull(
            function.parseHTTPMethod("GET"),
            function.parseHTTPMethod("POST"),
            function.parseHTTPMethod("PUT"),
            function.parseHTTPMethod("DELETE"),
            function.parseHTTPMethod("PATCH"),
            function.parseHTTPMethod("HEAD"),
            function.parseHTTPMethod("OPTIONS"),
            function.parseHTTPMethod("HTTP"),
        )
    }

    private fun extractKtorGenFunction(declaration: KSFunctionDeclaration): FunctionGenerationOptions? =
        declaration.getAnnotation<KtorGenFunction, FunctionGenerationOptions>(manualExtraction = {
            FunctionGenerationOptions(
                goingToGenerate = it.getArgumentValueByName("generate") ?: true,
                customHeader = it.getArgumentValueByName("customHeader"),
            )
        }) {
            FunctionGenerationOptions(
                goingToGenerate = it.generate,
                customHeader = it.customHeader,
            )
        }
}
