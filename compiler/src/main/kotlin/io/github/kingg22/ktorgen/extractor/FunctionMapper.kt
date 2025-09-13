package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import io.github.kingg22.ktorgen.DiagnosticSender
import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.core.KtorGenExperimental
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
import io.github.kingg22.ktorgen.requireNotNull
import io.github.kingg22.ktorgen.work

class FunctionMapper : DeclarationFunctionMapper {
    override fun mapToModel(
        declaration: KSFunctionDeclaration,
        onAddImport: (String) -> Unit,
        basePath: String,
        timer: (String) -> DiagnosticSender,
    ): Pair<FunctionData?, List<KSAnnotated>> {
        val name = declaration.simpleName.asString()
        val deferredSymbols = mutableListOf<KSAnnotated>()
        val timer = timer("Function Mapper for [$name]")
        return timer.work { _ ->
            timer.addStep("Extracting the KtorGenFunction annotation")
            var options = extractKtorGenFunction(declaration) ?: FunctionGenerationOptions.DEFAULT

            if (options.propagateAnnotations) {
                options = updateFunctionOptionsWith(declaration, options, timer, deferredSymbols)
            }

            if (!options.goingToGenerate) timer.addStep("Early finish processing, not going to generate this function.")

            val type = timer.requireNotNull(
                declaration.returnType?.resolve(),
                KtorGenLogger.FUNCTION_NOT_RETURN_TYPE + name,
                declaration,
            )
            if (type.isError) deferredSymbols += type.declaration

            val returnType = TypeData(type)
            timer.addStep(
                "Processed return type: ${returnType.parameterType.declaration.simpleName.asString()}",
            )

            val functionAnnotations = if (options.goingToGenerate) {
                getFunctionAnnotations(
                    declaration,
                    timer.createTask("Extract annotations"),
                    basePath,
                )
            } else {
                listOf(FunctionAnnotation.HttpMethodAnnotation(basePath, HttpMethod.Absent))
            }

            timer.addStep("Processed function annotations, adding imports")
            addImportsForFunctionAnnotations(functionAnnotations, onAddImport)

            val parameters = if (options.goingToGenerate) {
                declaration.parameters.mapNotNull { param ->
                    val (parameterData, symbols) = DeclarationParameterMapper.DEFAULT.mapToModel(
                        param,
                        timer::createTask,
                    )
                    parameterData?.let {
                        timer.addStep("Processed param: ${it.nameString}")
                        return@mapNotNull parameterData
                    }
                    deferredSymbols += symbols
                    return@mapNotNull null
                }
            } else {
                emptyList()
            }

            timer.addStep("Adding imports of parameters")
            addImportsForParametersAnnotations(parameters.flatMap { p -> p.ktorgenAnnotations }, onAddImport)

            val isSuspend = declaration.modifiers.contains(Modifier.SUSPEND)
            val modifiers = buildSet {
                add(KModifier.OVERRIDE)
                if (isSuspend) add(KModifier.SUSPEND)
            }

            timer.addStep("Finishing mapping")

            if (deferredSymbols.isNotEmpty()) {
                timer.addStep("Found deferred symbols, skipping to next round of processing")
                return@work null to deferredSymbols
            }

            FunctionData(
                name = name,
                returnTypeData = returnType,
                isSuspend = isSuspend,
                isImplemented = declaration.isAbstract.not(),
                parameterDataList = parameters,
                ktorGenAnnotations = functionAnnotations,
                httpMethodAnnotation =
                functionAnnotations.filterIsInstance<FunctionAnnotation.HttpMethodAnnotation>().first(),
                modifierSet = modifiers,
                ksFunctionDeclaration = declaration,
                options = options,
            ) to emptyList()
        }
    }

    private fun updateFunctionOptionsWith(
        declaration: KSFunctionDeclaration,
        options: FunctionGenerationOptions,
        timer: DiagnosticSender,
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

    @OptIn(KspExperimental::class)
    private fun getFunctionAnnotations(function: KSFunctionDeclaration, timer: DiagnosticSender, basePath: String) =
        timer.work {
            buildList {
                timer.addStep("Start collect KtorGen annotations, first Http Method")

                val method = httpAnnotationResolver(function, basePath)
                if (method.isEmpty()) {
                    add(FunctionAnnotation.HttpMethodAnnotation(basePath, HttpMethod.Absent))
                    timer.addStep("Http method not found, adding absent value, need validation!", function)
                } else {
                    require(method.size == 1) {
                        "${KtorGenLogger.ONLY_ONE_HTTP_METHOD_IS_ALLOWED} Found: ${
                            method.joinToString {
                                it.httpMethod.value
                            }
                        } at ${function.simpleName.asString()}"
                    }
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
                function.getAnnotationsByType(Multipart::class).firstOrNull()?.let {
                    add(FunctionAnnotation.Multipart)
                    timer.addStep("Multipart found")
                }

                timer.addStep("Going to get FormUrlEncoded")
                function.getAnnotationsByType(FormUrlEncoded::class).firstOrNull()?.let {
                    add(FunctionAnnotation.FormUrlEncoded)
                    timer.addStep("FormUrlEncoded found")
                }

                timer.addStep("Going to get Headers")
                function.getAnnotationsByType(Header::class)
                    .map { it.name to it.value }
                    .toList()
                    .takeIf(List<*>::isNotEmpty)
                    ?.let { headers ->
                        timer.addStep("Header found")
                        add(FunctionAnnotation.Headers(headers))
                    }

                timer.addStep("Going to get Cookies")
                function.getAnnotationsByType(Cookie::class)
                    .map { it.toCookieValues() }
                    .toList()
                    .takeIf(List<*>::isNotEmpty)
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
                this.getAnnotationsByType(HTTP::class).firstOrNull()?.let {
                    FunctionAnnotation.HttpMethodAnnotation(basePath + it.path, HttpMethod.parse(it.method.uppercase()))
                }
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

    @OptIn(KtorGenExperimental::class)
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
