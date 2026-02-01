package io.github.kingg22.ktorgen.generator

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.UNIT
import io.github.kingg22.ktorgen.applyIf
import io.github.kingg22.ktorgen.model.FunctionData
import io.github.kingg22.ktorgen.model.HttpRequestBuilderTypeName
import io.github.kingg22.ktorgen.model.HttpStatementClassName
import io.github.kingg22.ktorgen.model.RESULT_CLASS
import io.github.kingg22.ktorgen.model.annotations.CookieValues
import io.github.kingg22.ktorgen.model.annotations.FunctionAnnotation
import io.github.kingg22.ktorgen.model.annotations.HttpMethod
import io.github.kingg22.ktorgen.model.annotations.ParameterAnnotation
import io.github.kingg22.ktorgen.model.annotations.removeWhitespace
import io.github.kingg22.ktorgen.model.isFlowType
import io.github.kingg22.ktorgen.model.isHttpRequestBuilderType
import io.github.kingg22.ktorgen.model.isHttpStatementType
import io.github.kingg22.ktorgen.model.isResultType
import io.github.kingg22.ktorgen.model.unwrapFlow
import io.github.kingg22.ktorgen.model.unwrapFlowResult
import io.github.kingg22.ktorgen.model.unwrapResult

internal class FunctionBodyGenerator(
    private val httpClient: MemberName,
    private val parameterGenerator: ParameterBodyGenerator,
) {
    fun generateFunctionBody(func: FunctionData): CodeBlock {
        val returnType = func.returnTypeData.typeName
        val isFlow = returnType.isFlowType()

        return when {
            isFlow && returnType.unwrapFlow().isResultType() -> generateFlowResultBody(returnType, func)

            isFlow -> generateFlowBody(returnType, func)

            returnType.isResultType() &&
                returnType.unwrapResult().isHttpRequestBuilderType -> generateResultBuilderBody(func)

            returnType.isResultType() &&
                returnType.unwrapResult().isHttpStatementType -> generateResultHttpStatementBody(func)

            returnType.isResultType() -> generateResultBody(returnType, func)

            returnType.isHttpRequestBuilderType -> generateBuilderBody(func)

            returnType.isHttpStatementType -> generateHttpStatementBody(func)

            else -> generateSimpleBody(returnType, func)
        }
    }

    private fun generateResultBuilderBody(func: FunctionData): CodeBlock = CodeBlock.builder().apply {
        beginControlFlow("return try")
        beginControlFlow("%T().apply", HttpRequestBuilderTypeName)
            .buildRequest(func)
        endControlFlow()
        addStatement(".let { _b -> %T.success(_b) }", RESULT_CLASS)
        nextControlFlow("catch (_exception: %T)", KOTLIN_EXCEPTION_CLASS)
            .applyIf(func.isSuspend) {
                addStatement("%M().%M()", COROUTINES_CURRENT_CONTEXT, COROUTINES_CONTEXT_ENSURE_ACTIVE)
            }
            .addStatement("%T.failure(_exception)", RESULT_CLASS)
        endControlFlow()
    }.build()

    private fun generateBuilderBody(func: FunctionData): CodeBlock = CodeBlock.builder().apply {
        beginControlFlow("return %T().apply", HttpRequestBuilderTypeName)
            .buildRequest(func)
        endControlFlow()
    }.build()

    private fun generateResultHttpStatementBody(func: FunctionData): CodeBlock = CodeBlock.builder().apply {
        beginControlFlow("return try")
        beginControlFlow("val _requestBuilder = %T().apply", HttpRequestBuilderTypeName)
            .buildRequest(func)
        endControlFlow()
        addStatement(
            "%T.success(%T(_requestBuilder, %M))",
            RESULT_CLASS,
            HttpStatementClassName,
            httpClient,
        )
        nextControlFlow("catch (_exception: %T)", KOTLIN_EXCEPTION_CLASS)
            .applyIf(func.isSuspend) {
                addStatement("%M().%M()", COROUTINES_CURRENT_CONTEXT, COROUTINES_CONTEXT_ENSURE_ACTIVE)
            }
            .addStatement("%T.failure(_exception)", RESULT_CLASS)
        endControlFlow()
    }.build()

    private fun generateHttpStatementBody(func: FunctionData) = CodeBlock.builder().apply {
        beginControlFlow("val _requestBuilder = %T().apply", HttpRequestBuilderTypeName)
            .buildRequest(func)
        endControlFlow()
        addStatement("return %T(_requestBuilder, %M)", HttpStatementClassName, httpClient)
    }.build()

    private fun generateFlowResultBody(type: ParameterizedTypeName, func: FunctionData) = CodeBlock.builder().apply {
        beginControlFlow("return %M", FLOW_MEMBER)
            .beginControlFlow("try")
            .addRequest(func)
        if (type.unwrapFlowResult() != UNIT) {
            add(BODY_TYPE, BODY_FUNCTION, type.unwrapFlowResult())
            beginControlFlow(LET_RESULT)
                .addStatement("this.emit(%T.success(_result))", RESULT_CLASS)
            endControlFlow()
        } else {
            addStatement("this.emit(%T.success(%T))", RESULT_CLASS, UNIT)
        }
        nextControlFlow("catch (_exception: %T)", KOTLIN_EXCEPTION_CLASS)
            .addStatement("%M().%M()", COROUTINES_CURRENT_CONTEXT, COROUTINES_CONTEXT_ENSURE_ACTIVE)
            .addStatement("this.emit(%T.failure(_exception))", RESULT_CLASS)
        endControlFlow()
        endControlFlow()
    }.build()

    private fun generateFlowBody(returnType: ParameterizedTypeName, func: FunctionData) = CodeBlock.builder().apply {
        beginControlFlow("return %M", FLOW_MEMBER)
        addRequest(func)
        if (returnType.unwrapFlow() != UNIT) {
            add(BODY_TYPE, BODY_FUNCTION, returnType.unwrapFlow())
            beginControlFlow(LET_RESULT)
                .addStatement("this.emit(_result)")
            endControlFlow()
        } else {
            addStatement("this.emit(%T)", UNIT)
        }
        endControlFlow()
    }.build()

    private fun generateResultBody(returnType: ParameterizedTypeName, func: FunctionData) = CodeBlock.builder().apply {
        beginControlFlow("return try")
            .addRequest(func)
        if (returnType.unwrapResult() != UNIT) {
            addStatement(BODY_TYPE, BODY_FUNCTION, returnType.unwrapResult())
            beginControlFlow(LET_RESULT)
                .addStatement("%T.success(_result)", RESULT_CLASS)
            endControlFlow()
        } else {
            addStatement("%T.success(%T)", RESULT_CLASS, UNIT)
        }
        nextControlFlow("catch (_exception: %T)", KOTLIN_EXCEPTION_CLASS)
            .addStatement("%M().%M()", COROUTINES_CURRENT_CONTEXT, COROUTINES_CONTEXT_ENSURE_ACTIVE)
            .addStatement("%T.failure(_exception)", RESULT_CLASS)
        endControlFlow()
    }.build()

    /** This generation is terminal and relays on the body method */
    private fun generateSimpleBody(returnType: TypeName, func: FunctionData): CodeBlock = CodeBlock.builder().apply {
        val isUnit = returnType == UNIT
        if (!isUnit) add("return ")
        addRequest(func)
        if (!isUnit) add(".%M()", BODY_FUNCTION)
    }.build()

    /** This generation takes the HttpClient property and calls a request */
    private fun CodeBlock.Builder.addRequest(func: FunctionData) = apply {
        beginControlFlow("%M.%M", httpClient, KTOR_REQUEST_FUNCTION)
            .buildRequest(func)
        endControlFlow()
    }

    /** This generation needs a HttpRequestBuilder receiver */
    private fun CodeBlock.Builder.buildRequest(func: FunctionData) = apply {
        parameterGenerator.addBuilderCall(func.parameterDataList)
            .addHttpMethod(func)
            .addUrl(func)
            .addHeaders(func)
            .addCookies(func)
            .addBody(func)
        parameterGenerator.addAttributes(func.parameterDataList)
    }

    // -- core parts --
    private fun CodeBlock.Builder.addHttpMethod(func: FunctionData): CodeBlock.Builder {
        val httpMethod = func.httpMethodAnnotation.httpMethod
        return applyIf(httpMethod != HttpMethod.Absent) {
            if (httpMethod in HttpMethod.ktorMethods) {
                addStatement("this.method = %T.%L", KTOR_HTTP_METHOD, httpMethod.ktorMethodName)
                return@applyIf
            }
            addStatement("this.method = %T.parse(%S)", KTOR_HTTP_METHOD, httpMethod.value)
        }
    }

    private fun CodeBlock.Builder.addUrl(func: FunctionData) = apply {
        val content = CodeBlock.builder().apply {
            // Url takeFrom
            func.parameterDataList.firstOrNull { it.hasAnnotation<ParameterAnnotation.Url>() }?.let {
                addStatement(THIS_MEMBER_LITERAL, KTOR_URL_TAKE_FROM_FUNCTION, it.nameString)
            }

            // Url template of the http method
            if (func.urlTemplate.isNotEmpty) {
                val urlArgs = func.urlTemplate.keys.map { key ->
                    val param = func.parameterDataList.first { parameter ->
                        parameter.findAnnotationOrNull<ParameterAnnotation.Path>()?.value == key
                    }

                    val paramName = param.nameString
                    val isEncoded = param.findAnnotationOrNull<ParameterAnnotation.Path>()?.encoded ?: false

                    if (isEncoded) {
                        CodeBlock.of("$%L", paramName) // ${userId}
                    } else {
                        // ${"$id".encodeURLPath()}
                        CodeBlock.of($$"${\"$%L\".%M()}", paramName, KTOR_URL_ENCODE_PATH)
                    }
                }

                // this takeFrom "some/url/${userId}/${postId}"
                addStatement(
                    THIS_MEMBER_LITERAL,
                    KTOR_URL_TAKE_FROM_FUNCTION,
                    CodeBlock.builder()
                        .add("\"\"\"")
                        .add(func.urlTemplate.template, *urlArgs.toTypedArray())
                        .add("\"\"\"")
                        .build(),
                )
            }

            // Query of the url like ?a=1&hello
            parameterGenerator.addQuery(func.parameterDataList)

            // Add fragment like end of the url #header or #function
            add(getFragmentTextBlock(func))
        }

        if (content.isNotEmpty()) {
            beginControlFlow("this.url { _ ->")
                .add(content.build())
            endControlFlow()
        }
    }

    private fun CodeBlock.Builder.addHeaders(func: FunctionData) = apply {
        // multipart Ktor handle content type

        if (func.isFormUrl) {
            addStatement(
                "this.%M(%T.Application.FormUrlEncoded)", // this.contentType ContentType.Application.FormUrlEncoded
                KTOR_CONTENT_TYPE_FUNCTION,
                KTOR_CONTENT_TYPE_CLASS,
            )
        }

        parameterGenerator.addHeaderParameter(func.parameterDataList)

        parameterGenerator.addHeaderMap(func.parameterDataList)

        func.findAnnotationOrNull<FunctionAnnotation.Headers>()?.value?.associate { (name, value) ->
            name.removeWhitespace() to value.removeWhitespace()
        }?.takeIf { it.isNotEmpty() }?.let { headers ->
            beginControlFlow(THIS_HEADERS, KTOR_CLIENT_REQUEST_HEADER_FUNCTION)
            for ((key, value) in headers) {
                addStatement("this.append(%S, %S)", key, value)
            }
            endControlFlow()
        }
    }

    private fun CodeBlock.Builder.addCookies(func: FunctionData) = apply {
        // Recolectamos todos los candidatos a cookies (de función y parámetros)
        func.ktorGenAnnotations.filterIsInstance<FunctionAnnotation.Cookies>()
            .flatMap { it.value }
            .map { CookieCandidate(it) }
            .plus(
                // Cookies en parámetros
                func.parameterDataList
                    .flatMap { p ->
                        p.ktorgenAnnotations
                            .filterIsInstance<ParameterAnnotation.Cookies>()
                            .flatMap { it.value }
                            .map { cookie -> CookieCandidate(cookie, p.isVararg, p.nameString) }
                    },
            ).forEach { c ->
                if (c.useVarargItem && c.varargParamName != null) {
                    beginControlFlow(LITERAL_FOREACH, c.varargParamName)
                        .addCookieStatement(c.cookieValues, useVarargItem = true)
                    endControlFlow()
                } else {
                    addCookieStatement(c.cookieValues)
                }
            }
    }

    private fun CodeBlock.Builder.addCookieStatement(cookieValues: CookieValues, useVarargItem: Boolean = false) {
        addStatement("this.%M(", KTOR_REQUEST_COOKIE_FUNCTION)
        indent()
            .addStatement("name = %S,", cookieValues.name)
            .addStatement(
                "value = %P,",
                if (cookieValues.isValueParameter) {
                    if (useVarargItem) $$"$it" else "$${cookieValues.value}"
                } else {
                    cookieValues.value
                },
            )
            .addStatement("maxAge = %L,", cookieValues.maxAge)
            .addStatement(
                "expires = %L,",
                cookieValues.expiresTimestamp?.let { t -> CodeBlock.of("%T(%L)", KTOR_GMT_DATE_CLASS, t) },
            )
            .addStatement("domain = %P,", cookieValues.domain)
            .addStatement("path = %P,", cookieValues.path)
            .addStatement("secure = %L,", cookieValues.secure)
            .addStatement("httpOnly = %L,", cookieValues.httpOnly)

        if (cookieValues.extensions.isNotEmpty()) {
            addStatement("extensions = %M(", KOTLIN_MAP_OF)
            indent()
            cookieValues.extensions.forEach { (key, value) ->
                addStatement("%P %M %P,", key, KOTLIN_TO_PAIR_FUNCTION, value)
            }
            unindent()
            addStatement("),")
        } else {
            addStatement("extensions = %M(),", KOTLIN_EMPTY_MAP)
        }

        unindent()
        addStatement(")")
    }

    private class CookieCandidate(
        val cookieValues: CookieValues,
        val useVarargItem: Boolean = false,
        val varargParamName: String? = null,
    )

    private fun CodeBlock.Builder.addBody(func: FunctionData) = apply {
        if (func.isBody) {
            addStatement(
                THIS_MEMBER_LITERAL,
                KTOR_REQUEST_SET_BODY_FUNCTION,
                func.parameterDataList.first { it.hasAnnotation<ParameterAnnotation.Body>() }.nameString,
            )
        }
        if (func.isMultipart) add(parameterGenerator.getPartsCodeBlock(func.parameterDataList))
        if (func.isFormUrl) add(parameterGenerator.getFieldArgumentsCodeBlock(func.parameterDataList))
    }

    private fun getFragmentTextBlock(func: FunctionData) = CodeBlock.builder().apply {
        // Only ONE fragment per function is accepted
        func.findAnnotationOrNull<FunctionAnnotation.Fragment>()?.let {
            addStatement("this.%L = %P", if (it.encoded) "encodedFragment" else "fragment", it.value)
        } ?: parameterGenerator.addFragmentUrl(func)
    }.build()
}
