package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.KModifier
import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.extractor.DeclarationParameterMapper.Companion.getArgumentValueByName
import io.github.kingg22.ktorgen.http.FormUrlEncoded
import io.github.kingg22.ktorgen.http.HTTP
import io.github.kingg22.ktorgen.http.Headers
import io.github.kingg22.ktorgen.http.Multipart
import io.github.kingg22.ktorgen.http.Streaming
import io.github.kingg22.ktorgen.model.FunctionData
import io.github.kingg22.ktorgen.model.KTOR_CLIENT_ATTRIBUTE_KEY
import io.github.kingg22.ktorgen.model.KTOR_CLIENT_FORM_DATA
import io.github.kingg22.ktorgen.model.KTOR_CLIENT_FORM_DATA_CONTENT
import io.github.kingg22.ktorgen.model.KTOR_CLIENT_HEADERS
import io.github.kingg22.ktorgen.model.KTOR_CLIENT_MULTI_PART_FORM_DATA_CONTENT
import io.github.kingg22.ktorgen.model.KTOR_CLIENT_SET_BODY
import io.github.kingg22.ktorgen.model.KTOR_ENCODE_URL_PATH
import io.github.kingg22.ktorgen.model.KTOR_HTTP_METHOD
import io.github.kingg22.ktorgen.model.KTOR_PARAMETERS
import io.github.kingg22.ktorgen.model.ReturnType
import io.github.kingg22.ktorgen.model.annotations.FunctionAnnotation
import io.github.kingg22.ktorgen.model.annotations.HttpMethod
import io.github.kingg22.ktorgen.model.annotations.ParameterAnnotation

class FunctionMapper : DeclarationFunctionMapper {
    override fun mapToModel(declaration: KSFunctionDeclaration, onAddImport: (String) -> Unit): FunctionData {
        val name = declaration.simpleName.asString()
        val parameters = declaration.parameters.map { param ->
            DeclarationParameterMapper.DEFAULT.mapToModel(param)
        }.also {
            for (param in it) {
                for (annotation in param.annotations) {
                    when (annotation) {
                        ParameterAnnotation.Body,
                        is ParameterAnnotation.Part,
                        is ParameterAnnotation.PartMap,
                        is ParameterAnnotation.Field,
                        is ParameterAnnotation.FieldMap,
                        -> onAddImport(KTOR_CLIENT_SET_BODY)

                        is ParameterAnnotation.Header, ParameterAnnotation.HeaderMap -> onAddImport(KTOR_CLIENT_HEADERS)
                        is ParameterAnnotation.Path -> if (!annotation.encoded) onAddImport(KTOR_ENCODE_URL_PATH)
                        is ParameterAnnotation.Tag -> onAddImport(KTOR_CLIENT_ATTRIBUTE_KEY)

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
        }
        val returnType = ReturnType(
            requireNotNull(declaration.returnType?.resolve()) { "Function $name has no return type" },
        )
        val httpAnnotations = httpAnnotationResolver(declaration)

        require(httpAnnotations.isNotEmpty()) { KtorGenLogger.NO_HTTP_ANNOTATION_AT + name }

        require(httpAnnotations.size == 1) {
            "${KtorGenLogger.ONLY_ONE_HTTP_METHOD_IS_ALLOWED} Found: ${
                httpAnnotations.joinToString { it.httpMethod.value }
            } at $name"
        }

        onAddImport(KTOR_HTTP_METHOD)

        val functionAnnotations = getFunctionAnnotations(declaration).also {
            for (annotation in it) {
                if (annotation is FunctionAnnotation.Headers ||
                    annotation is FunctionAnnotation.FormUrlEncoded
                ) {
                    onAddImport(KTOR_CLIENT_HEADERS)
                }

                if (annotation is FunctionAnnotation.FormUrlEncoded ||
                    annotation is FunctionAnnotation.Multipart ||
                    parameters.any { param ->
                        param.hasAnnotation<ParameterAnnotation.Field>() ||
                            param.hasAnnotation<ParameterAnnotation.Part>()
                    }
                ) {
                    onAddImport(KTOR_CLIENT_FORM_DATA_CONTENT)
                    onAddImport(KTOR_CLIENT_MULTI_PART_FORM_DATA_CONTENT)
                    onAddImport(KTOR_CLIENT_FORM_DATA)
                    onAddImport(KTOR_PARAMETERS)
                }
            }
        }

        val isSuspend = declaration.modifiers.contains(Modifier.SUSPEND)

        val modifiers = buildList {
            add(KModifier.OVERRIDE)
            if (isSuspend) add(KModifier.SUSPEND)
        }

        return FunctionData(
            name = name,
            returnType = returnType,
            isSuspend = isSuspend,
            isImplemented = declaration.isAbstract.not(),
            parameterDataList = parameters,
            httpMethodAnnotation = httpAnnotations.first(),
            ktorGenAnnotations = functionAnnotations,
            nonKtorGenAnnotations = emptyList(), // TODO
            modifiers = modifiers,
        )
    }

    @OptIn(KspExperimental::class)
    private fun getFunctionAnnotations(function: KSFunctionDeclaration) = buildList {
        function.getAnnotationsByType(Headers::class).firstOrNull()?.let { headers ->
            add(FunctionAnnotation.Headers(headers.value.toList()))
        }
        function.getAnnotationsByType(Multipart::class).firstOrNull()?.let { add(FunctionAnnotation.Multipart) }
        function.getAnnotationsByType(Streaming::class).firstOrNull()?.let { add(FunctionAnnotation.Streaming) }
        function.getAnnotationsByType(FormUrlEncoded::class).firstOrNull()?.let {
            add(FunctionAnnotation.FormUrlEncoded)
        }
    }

    fun httpAnnotationResolver(function: KSFunctionDeclaration): List<FunctionAnnotation.HttpMethodAnnotation> {
        @OptIn(KspExperimental::class)
        fun KSFunctionDeclaration.parseHTTPMethod(name: String): FunctionAnnotation.HttpMethodAnnotation? {
            val annotation = this.annotations.firstOrNull { it.shortName.asString() == name }
            return if (annotation == null) {
                null
            } else if (name == "HTTP") {
                this.getAnnotationsByType(HTTP::class).firstOrNull()?.let {
                    FunctionAnnotation.HttpMethodAnnotation(it.path, HttpMethod.parse(it.method))
                }
            } else {
                val path = annotation.getArgumentValueByName<Any?>("value")
                val value = when (path) {
                    is String -> path
                    is ArrayList<*> -> path.firstOrNull() as? String
                    else -> null
                }
                FunctionAnnotation.HttpMethodAnnotation(value.orEmpty(), HttpMethod.parse(name))
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
