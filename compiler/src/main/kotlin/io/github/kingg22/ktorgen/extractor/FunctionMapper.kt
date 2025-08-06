@file:OptIn(KtorGenExperimental::class)

package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.KModifier
import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.Timer
import io.github.kingg22.ktorgen.core.KtorGenExperimental
import io.github.kingg22.ktorgen.http.Cookie
import io.github.kingg22.ktorgen.http.FormUrlEncoded
import io.github.kingg22.ktorgen.http.Fragment
import io.github.kingg22.ktorgen.http.HTTP
import io.github.kingg22.ktorgen.http.Header
import io.github.kingg22.ktorgen.http.Multipart
import io.github.kingg22.ktorgen.http.Streaming
import io.github.kingg22.ktorgen.model.FunctionData
import io.github.kingg22.ktorgen.model.KTOR_CLIENT_ATTRIBUTE_KEY
import io.github.kingg22.ktorgen.model.KTOR_CLIENT_COOKIE
import io.github.kingg22.ktorgen.model.KTOR_CLIENT_FORM_DATA
import io.github.kingg22.ktorgen.model.KTOR_CLIENT_FORM_DATA_CONTENT
import io.github.kingg22.ktorgen.model.KTOR_CLIENT_HEADERS
import io.github.kingg22.ktorgen.model.KTOR_CLIENT_MULTI_PART_FORM_DATA_CONTENT
import io.github.kingg22.ktorgen.model.KTOR_CLIENT_SET_BODY
import io.github.kingg22.ktorgen.model.KTOR_CONTENT_TYPE
import io.github.kingg22.ktorgen.model.KTOR_CONTENT_TYPE_ADD
import io.github.kingg22.ktorgen.model.KTOR_ENCODE_URL_PATH
import io.github.kingg22.ktorgen.model.KTOR_GMT_DATE
import io.github.kingg22.ktorgen.model.KTOR_HTTP_METHOD
import io.github.kingg22.ktorgen.model.KTOR_PARAMETERS
import io.github.kingg22.ktorgen.model.TypeData
import io.github.kingg22.ktorgen.model.annotations.FunctionAnnotation
import io.github.kingg22.ktorgen.model.annotations.HttpMethod
import io.github.kingg22.ktorgen.model.annotations.ParameterAnnotation
import io.github.kingg22.ktorgen.model.annotations.toCookieValues

class FunctionMapper : DeclarationFunctionMapper {
    override fun mapToModel(declaration: KSFunctionDeclaration, onAddImport: (String) -> Unit): FunctionData {
        val name = declaration.simpleName.asString()
        val timer = Timer("KtorGen [Function Mapper] for $name").start()
        try {
            val parameters = declaration.parameters.map { param ->
                DeclarationParameterMapper.DEFAULT.mapToModel(param).also {
                    timer.markStepCompleted("Processed param: ${it.nameString}")
                }
            }
            timer.markStepCompleted("Adding imports of parameters")
            addImportsForParametersAnnotations(parameters.flatMap { p -> p.ktorgenAnnotations }, onAddImport)

            val returnType = TypeData(
                requireNotNull(declaration.returnType?.resolve()) { KtorGenLogger.FUNCTION_NOT_RETURN_TYPE + name },
            )
            timer.markStepCompleted(
                "Processed return type: ${returnType.parameterType.declaration.simpleName.asString()}",
            )

            val functionAnnotations = getFunctionAnnotations(declaration, timer)
            timer.markStepCompleted("Processed function annotations, adding imports")
            addImportsForFunctionAnnotations(functionAnnotations, onAddImport)

            val isSuspend = declaration.modifiers.contains(Modifier.SUSPEND)

            val modifiers = buildSet {
                add(KModifier.OVERRIDE)
                if (isSuspend) add(KModifier.SUSPEND)
            }

            timer.markStepCompleted("Finishing mapping")
            return FunctionData(
                name = name,
                returnTypeData = returnType,
                isSuspend = isSuspend,
                isImplemented = declaration.isAbstract.not(),
                parameterDataList = parameters,
                ktorGenAnnotations = functionAnnotations,
                httpMethodAnnotation =
                functionAnnotations.filterIsInstance<FunctionAnnotation.HttpMethodAnnotation>().first(),
                nonKtorGenAnnotations = emptyList(), // TODO
                modifierSet = modifiers,
                ksFunctionDeclaration = declaration,
            )
        } catch (e: Exception) {
            timer.markStepCompleted("Error on function: ${declaration.simpleName.asString()}")
            throw e
        } finally {
            timer.finishAndPrint()
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
                is FunctionAnnotation.Fragment, FunctionAnnotation.Streaming -> {
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
    private fun getFunctionAnnotations(function: KSFunctionDeclaration, timer: Timer) = buildList {
        timer.markStepCompleted("Start collect KtorGen annotations, first Http Method")

        val method = httpAnnotationResolver(function)
        if (method.isEmpty()) {
            add(FunctionAnnotation.HttpMethodAnnotation("", HttpMethod.Absent))
            timer.markStepCompleted("Http method not found, adding absent value, need validation!")
        } else {
            require(method.size == 1) {
                "${KtorGenLogger.ONLY_ONE_HTTP_METHOD_IS_ALLOWED} Found: ${method.joinToString {
                    it.httpMethod.value
                }} at ${function.simpleName.asString()}"
            }
            val http = method.first()
            add(http)
            timer.markStepCompleted("Processed http annotation $http")
        }

        timer.markStepCompleted("Going to get Fragment")
        function.getAnnotation<Fragment>(
            manualExtraction = {
                add(
                    FunctionAnnotation.Fragment(
                        it.getArgumentValueByName<String>("value").orEmpty(),
                        it.getArgumentValueByName("encoded") ?: false,
                    ),
                )
                timer.markStepCompleted("Fragment found")
            },
        ) {
            add(FunctionAnnotation.Fragment(it.value, it.encoded))
            timer.markStepCompleted("Fragment found")
        }

        timer.markStepCompleted("Going to get Multipart")
        function.getAnnotationsByType(Multipart::class).firstOrNull()?.let {
            add(FunctionAnnotation.Multipart)
            timer.markStepCompleted("Multipart found")
        }

        timer.markStepCompleted("Going to get Streaming")
        function.getAnnotationsByType(Streaming::class).firstOrNull()?.let {
            add(FunctionAnnotation.Streaming)
            timer.markStepCompleted("Streaming found")
        }

        timer.markStepCompleted("Going to get FormUrlEncoded")
        function.getAnnotationsByType(FormUrlEncoded::class).firstOrNull()?.let {
            add(FunctionAnnotation.FormUrlEncoded)
            timer.markStepCompleted("FormUrlEncoded found")
        }

        timer.markStepCompleted("Going to get Headers")
        function.getAnnotationsByType(Header::class)
            .map { it.name to it.value }
            .toList()
            .takeIf(List<*>::isNotEmpty)
            ?.let { headers ->
                timer.markStepCompleted("Header found")
                add(FunctionAnnotation.Headers(headers))
            }

        timer.markStepCompleted("Going to get Cookies")
        function.getAnnotationsByType(Cookie::class)
            .map(Cookie::toCookieValues)
            .toList()
            .takeIf(List<*>::isNotEmpty)
            ?.let { cookies ->
                timer.markStepCompleted("Cookies found")
                add(FunctionAnnotation.Cookies(cookies))
            }

        timer.markStepCompleted("Finish collection of KtorGen annotations")
    }

    private fun httpAnnotationResolver(function: KSFunctionDeclaration): List<FunctionAnnotation.HttpMethodAnnotation> {
        @OptIn(KspExperimental::class)
        fun KSFunctionDeclaration.parseHTTPMethod(name: String): FunctionAnnotation.HttpMethodAnnotation? {
            val annotation = this.annotations.firstOrNull { it.shortName.asString() == name }
            return if (annotation == null) {
                null
            } else if (name == "HTTP") {
                this.getAnnotationsByType(HTTP::class).firstOrNull()?.let {
                    FunctionAnnotation.HttpMethodAnnotation(it.path, HttpMethod.parse(it.method.uppercase()))
                }
            } else {
                val value = when (val path = annotation.getArgumentValueByName<Any?>("value")) {
                    is String -> path
                    is ArrayList<*> -> path.firstOrNull() as? String
                    else -> null
                }
                FunctionAnnotation.HttpMethodAnnotation(value.orEmpty(), HttpMethod.parse(name.uppercase()))
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
}
