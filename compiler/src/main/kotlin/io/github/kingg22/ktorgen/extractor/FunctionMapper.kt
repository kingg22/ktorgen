package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ksp.toKModifier
import io.github.kingg22.ktorgen.DiagnosticSender
import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.core.KtorGenFunction
import io.github.kingg22.ktorgen.http.Cookie
import io.github.kingg22.ktorgen.http.FormUrlEncoded
import io.github.kingg22.ktorgen.http.Fragment
import io.github.kingg22.ktorgen.http.HTTP
import io.github.kingg22.ktorgen.http.Header
import io.github.kingg22.ktorgen.http.Multipart
import io.github.kingg22.ktorgen.model.*
import io.github.kingg22.ktorgen.model.annotations.FunctionAnnotation
import io.github.kingg22.ktorgen.model.annotations.HttpMethod
import io.github.kingg22.ktorgen.model.annotations.ParameterAnnotation
import io.github.kingg22.ktorgen.model.annotations.toCookieValues
import io.github.kingg22.ktorgen.require
import io.github.kingg22.ktorgen.requireNotNull
import io.github.kingg22.ktorgen.work

internal class FunctionMapper : DeclarationFunctionMapper {
    context(timer: DiagnosticSender)
    override fun mapToModel(
        declaration: KSFunctionDeclaration,
        onAddImport: (String) -> Unit,
        basePath: String,
    ): Pair<FunctionData?, List<KSAnnotated>> {
        val name = declaration.simpleName.asString()
        val deferredSymbols = mutableListOf<KSAnnotated>()
        return timer.work {
            timer.addStep("Extracting the KtorGenFunction annotation")
            var options = extractKtorGenFunction(declaration) ?: FunctionGenerationOptions.DEFAULT

            if (options.propagateAnnotations) {
                options = updateFunctionOptionsWith(declaration, options, deferredSymbols)
            }

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
            timer.addStep(
                "Processed return type: ${returnType.parameterType.declaration.simpleName.asString()}",
            )

            val functionAnnotations = context(timer.createTask("Extract annotations")) {
                getFunctionAnnotations(declaration, basePath)
            }

            timer.addStep("Processed function annotations, adding imports")
            addImportsForFunctionAnnotations(functionAnnotations, onAddImport)

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

            timer.addStep("Adding imports of parameters")
            addImportsForParametersAnnotations(parameters.flatMap { p -> p.ktorgenAnnotations }, onAddImport)

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
                parameterDataList = parameters,
                ktorGenAnnotations = functionAnnotations,
                httpMethodAnnotation =
                functionAnnotations.filterIsInstance<FunctionAnnotation.HttpMethodAnnotation>().first(),
                modifierSet = modifiers.toSet(),
                ksFunctionDeclaration = declaration,
                options = options,
            ) to emptyList()
        }
    }

    context(timer: DiagnosticSender)
    private fun updateFunctionOptionsWith(
        declaration: KSFunctionDeclaration,
        options: FunctionGenerationOptions,
        deferredSymbols: MutableList<KSAnnotated>,
    ): FunctionGenerationOptions {
        timer.addStep("Extracting the rest of annotations propagated for function")

        val (annotations, optIns, symbols) = extractAnnotationsFiltered(declaration)

        timer.addStep(
            "Found ${symbols.size} unresolved symbols of function annotations, adding to deferred symbols list",
        )
        deferredSymbols += symbols

        val mergedOptIn = mergeOptIns(optIns, options.optIns)

        val mergedAnnotations = options.mergeAnnotations(annotations, optIns)

        return options.copy(
            annotations = mergedAnnotations,
            optInAnnotation = mergedOptIn,
            optIns = if (mergedOptIn != null) emptySet() else options.optIns,
        ).also {
            timer.addStep("Updated options with annotations and optIns propagated: $it")
        }
    }

    private fun addImportsForFunctionAnnotations(annotations: List<FunctionAnnotation>, onAddImport: (String) -> Unit) {
        for (annotation in annotations) {
            when (annotation) {
                is FunctionAnnotation.Headers -> {
                    onAddImport(KTOR_CLIENT_HEADERS)
                }

                is FunctionAnnotation.Cookies -> {
                    onAddImport(KTOR_CLIENT_COOKIE)
                    onAddImport(KTOR_GMT_DATE)
                }

                is FunctionAnnotation.FormUrlEncoded,
                is FunctionAnnotation.Multipart,
                -> {
                    onAddImport(KTOR_CONTENT_TYPE_ADD)
                    onAddImport(KTOR_CLIENT_FORM_DATA_CONTENT)
                    onAddImport(KTOR_CLIENT_MULTI_PART_FORM_DATA_CONTENT)
                    onAddImport(KTOR_CLIENT_FORM_DATA)
                    onAddImport(KTOR_PARAMETERS)
                }

                is FunctionAnnotation.HttpMethodAnnotation -> onAddImport(KTOR_HTTP_METHOD)
                is FunctionAnnotation.Fragment -> {
                    /* No Op */
                }
            }
        }
    }

    private fun addImportsForParametersAnnotations(
        parameters: List<ParameterAnnotation>,
        onAddImport: (String) -> Unit,
    ) {
        for (annotation in parameters) {
            when (annotation) {
                ParameterAnnotation.Body,
                is ParameterAnnotation.Part,
                is ParameterAnnotation.PartMap,
                is ParameterAnnotation.Field,
                is ParameterAnnotation.FieldMap,
                -> {
                    onAddImport(KTOR_CLIENT_SET_BODY)
                    onAddImport(KTOR_CONTENT_TYPE)
                    onAddImport(KTOR_CONTENT_TYPE_ADD)
                    onAddImport(KTOR_CLIENT_FORM_DATA_CONTENT)
                    onAddImport(KTOR_CLIENT_MULTI_PART_FORM_DATA_CONTENT)
                    onAddImport(KTOR_CLIENT_FORM_DATA)
                    onAddImport(KTOR_PARAMETERS)
                }

                is ParameterAnnotation.Header, ParameterAnnotation.HeaderMap -> onAddImport(
                    KTOR_CLIENT_HEADERS,
                )

                is ParameterAnnotation.Path -> if (!annotation.encoded) onAddImport(KTOR_ENCODE_URL_PATH)
                is ParameterAnnotation.Tag -> onAddImport(KTOR_CLIENT_ATTRIBUTE_KEY)
                is ParameterAnnotation.Cookies -> {
                    onAddImport(KTOR_CLIENT_COOKIE)
                    onAddImport(KTOR_GMT_DATE)
                }

                is ParameterAnnotation.Query,
                is ParameterAnnotation.QueryMap,
                is ParameterAnnotation.QueryName,
                ParameterAnnotation.Url,
                -> {
                    /* No Op*/
                }
            }
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
                timer.require(
                    method.size == 1,
                    "${KtorGenLogger.ONLY_ONE_HTTP_METHOD_IS_ALLOWED} Found: ${
                        method.joinToString {
                            it.httpMethod.value
                        }
                    } at ${function.simpleName.asString()}",
                )
                val http = method.first()
                add(http)
                timer.addStep("Processed http annotation $http")
            }

            timer.addStep("Going to get Fragment")
            function.getAnnotation<Fragment, Any>(manualExtraction = {
                add(
                    FunctionAnnotation.Fragment(
                        it.getArgumentValueByName<String>("value").orEmpty(),
                        it.getArgumentValueByName("encoded") ?: false,
                    ),
                )
                timer.addStep("Fragment found")
            }) {
                add(FunctionAnnotation.Fragment(it.value, it.encoded))
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

    private fun extractKtorGenFunction(declaration: KSFunctionDeclaration) =
        declaration.getAnnotation<KtorGenFunction, FunctionGenerationOptions>(manualExtraction = {
            FunctionGenerationOptions(
                generate = it.getArgumentValueByName("generate") ?: true,
                propagateAnnotations = it.getArgumentValueByName("propagateAnnotations") ?: true,
                annotationsToPropagate =
                it.getArgumentValueByName<List<KSType>>("annotations")
                    ?.mapNotNull { a -> a.declaration.qualifiedName?.asString() }
                    ?.map { n -> AnnotationSpec.builder(ClassName.bestGuess(n)).build() }
                    ?.toSet()
                    ?: emptySet(),
                optIns = it.getArgumentValueByName<List<KSType>>("optInAnnotations")
                    ?.mapNotNull { a -> a.declaration.qualifiedName?.asString() }
                    ?.map { n -> AnnotationSpec.builder(ClassName.bestGuess(n)).build() }
                    ?.toSet()
                    ?: emptySet(),
                customHeader = it.getArgumentValueByName("customHeader") ?: "",
                optInAnnotation = null,
            )
        }) {
            FunctionGenerationOptions(
                generate = it.generate,
                propagateAnnotations = it.propagateAnnotations,
                annotationsToPropagate = it.annotations.map { a -> AnnotationSpec.builder(a).build() }.toSet(),
                optIns = it.optInAnnotations.map { a -> AnnotationSpec.builder(a).build() }.toSet(),
                customHeader = it.customHeader,
                optInAnnotation = null,
            )
        }
}
