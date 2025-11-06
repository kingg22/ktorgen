package io.github.kingg22.ktorgen.generator

import com.squareup.kotlinpoet.*
import io.github.kingg22.ktorgen.KtorGenProcessor
import io.github.kingg22.ktorgen.KtorGenProcessor.Companion.arrayType
import io.github.kingg22.ktorgen.KtorGenProcessor.Companion.listType
import io.github.kingg22.ktorgen.applyIf
import io.github.kingg22.ktorgen.checkImplementation
import io.github.kingg22.ktorgen.model.*
import io.github.kingg22.ktorgen.model.annotations.CookieValues
import io.github.kingg22.ktorgen.model.annotations.FunctionAnnotation
import io.github.kingg22.ktorgen.model.annotations.HttpMethod
import io.github.kingg22.ktorgen.model.annotations.ParameterAnnotation
import io.github.kingg22.ktorgen.model.annotations.removeWhitespace

// TODO move functions of parameter to other class
internal class FunctionBodyGenerator(private val httpClient: MemberName) {
    // Snapshot must be initialized after the first round of processor
    private val partDataKtor = KtorGenProcessor.partDataKtor
    private companion object {
        private const val FORM_DATA_VARIABLE = "_multiPartDataContent"
        private const val PART_DATA_LIST_VARIABLE = "_partDataList"
        private const val FORM_DATA_CONTENT_VARIABLE = "_formDataContent"
        private const val KTOR_HTTP_PACKAGE = "io.ktor.http"
        private const val KOTLIN_COLLECTIONS_PACKAGE = "kotlin.collections"
        private val KTOR_PART_DATA_CLASS = ClassName("$KTOR_HTTP_PACKAGE.content", "PartData")
        private const val THIS_HEADERS = "this.%M"
        private val KTOR_CLIENT_REQUEST_HEADER_FUNCTION = MemberName(KTOR_CLIENT_REQUEST_PACKAGE, "headers")
        private const val LITERAL_FOREACH = "%L.forEach"
        private const val LITERAL_FOREACH_SAFE_NULL_ENTRY = "%L?.forEach { entry ->"
        private const val APPEND_STRING_LITERAL = "this.append(%S, %L)"
        private const val APPEND_STRING_STRING = "this.append(%S, %P)"
        private const val LITERAL_NN_LET = "%L?.let"
        private const val LET_RESULT = ".let { _result ->"
        private const val ITERABLE_FILTER_NULL_FOREACH = "%L?.filterNotNull()?.forEach"
        private const val ENTRY_VALUE_NN_LET = "entry.value?.let { value ->"
        private const val VALUE = $$"$value"
        private const val BODY_TYPE = ".%M<%T>()"
        private const val MEMBER_LITERAL = "%M(%L)"
        private const val THIS_MEMBER_LITERAL = "this.$MEMBER_LITERAL"
        private val BODY_FUNCTION = MemberName("io.ktor.client.call", "body", true)
        private const val ENCODED_PARAMETERS_APPEND = "this.encodedParameters.append"
        private const val PARAMETERS_APPEND = "this.parameters.append"
        private val FLOW_MEMBER = MemberName("kotlinx.coroutines.flow", "flow")
        private val KTOR_URL_TAKE_FROM_FUNCTION = MemberName(KTOR_HTTP_PACKAGE, "takeFrom", true)
        private val DECODE_URL_COMPONENTS_FUNCTION = MemberName(KTOR_HTTP_PACKAGE, "decodeURLQueryComponent", true)
        private val KTOR_HTTP_METHOD = ClassName(KTOR_HTTP_PACKAGE, "HttpMethod")
        private val KTOR_ATTRIBUTE_KEY = ClassName("io.ktor.util", "AttributeKey")
        private val KTOR_REQUEST_FUNCTION = MemberName(KTOR_CLIENT_REQUEST_PACKAGE, "request", true)
        private val KTOR_REQUEST_SET_BODY_FUNCTION = MemberName(KTOR_CLIENT_REQUEST_PACKAGE, "setBody", true)
        private val KTOR_URL_ENCODE_PATH = MemberName(KTOR_HTTP_PACKAGE, "encodeURLPath", true)
        private val KTOR_PARAMETERS_CLASS = ClassName(KTOR_HTTP_PACKAGE, "Parameters")
        private val KTOR_REQUEST_FORM_DATA_CONTENT_CLASS =
            ClassName("$KTOR_CLIENT_REQUEST_PACKAGE.forms", "FormDataContent")
        private val KTOR_REQUEST_MULTIPART_CONTENT_CLASS =
            ClassName("$KTOR_CLIENT_REQUEST_PACKAGE.forms", "MultiPartFormDataContent")
        private val KTOR_REQUEST_FORM_DATA_FUNCTION = MemberName("$KTOR_CLIENT_REQUEST_PACKAGE.forms", "formData")
        private val KOTLIN_LIST_OF = MemberName(KOTLIN_COLLECTIONS_PACKAGE, "listOf")
        private val KOTLIN_MAP_OF = MemberName(KOTLIN_COLLECTIONS_PACKAGE, "mapOf")
        private val KOTLIN_EMPTY_MAP = MemberName(KOTLIN_COLLECTIONS_PACKAGE, "mapOf")
        private val KTOR_GMT_DATE_CLASS = ClassName("io.ktor.util.date", "GMTDate")
        private val KTOR_REQUEST_COOKIE_FUNCTION = MemberName(KTOR_CLIENT_REQUEST_PACKAGE, "cookie", true)
        private val KTOR_CONTENT_TYPE_FUNCTION = MemberName(KTOR_HTTP_PACKAGE, "contentType", true)
        private val KTOR_CONTENT_TYPE_CLASS = ClassName(KTOR_HTTP_PACKAGE, "ContentType")
        private val KTOR_REQUEST_TAKE_FROM_FUNCTION = MemberName(KTOR_CLIENT_REQUEST_PACKAGE, "takeFrom", true)
        private val COROUTINES_CURRENT_CONTEXT = MemberName("kotlinx.coroutines", "currentCoroutineContext")
        private val COROUTINES_CONTEXT_ENSURE_ACTIVE = MemberName("kotlinx.coroutines", "ensureActive", true)
        private val KOTLIN_EXCEPTION_CLASS = ClassName("kotlin", "Exception")
        private val KOTLIN_PAIR_CLASS = ClassName("kotlin", "Pair")
    }

    fun generateFunctionBody(func: FunctionData): CodeBlock {
        val returnType = func.returnTypeData.typeName
        val isFlow = returnType.isFlowType()

        return when {
            isFlow && returnType.unwrapFlow().isResultType() -> generateFlowResultBody(returnType, func)
            isFlow -> generateFlowBody(returnType, func)
            returnType.isResultType() -> generateResultBody(returnType, func)
            else -> generateSimpleBody(returnType, func)
        }
    }

    private fun generateFlowResultBody(type: ParameterizedTypeName, func: FunctionData) = CodeBlock.builder().apply {
        beginControlFlow("return %M", FLOW_MEMBER)
            .beginControlFlow("try")
            .addRequest(func)
        if (type.unwrapFlowResult() != UNIT) {
            add(BODY_TYPE, BODY_FUNCTION, type.unwrapFlowResult())
            beginControlFlow(LET_RESULT)
                .addStatement("emit(%T.success(_result))", RESULT_CLASS)
            endControlFlow()
        } else {
            addStatement("emit(%T.success(%T))", RESULT_CLASS, UNIT)
        }
        nextControlFlow("catch (_exception: %T)", KOTLIN_EXCEPTION_CLASS)
            .addStatement("%M().%M()", COROUTINES_CURRENT_CONTEXT, COROUTINES_CONTEXT_ENSURE_ACTIVE)
            .addStatement("emit(%T.failure(_exception))", RESULT_CLASS)
        endControlFlow()
        endControlFlow()
    }.build()

    private fun generateFlowBody(returnType: ParameterizedTypeName, func: FunctionData) = CodeBlock.builder().apply {
        beginControlFlow("return %M", FLOW_MEMBER)
        addRequest(func)
        if (returnType.unwrapFlow() != UNIT) {
            add(BODY_TYPE, BODY_FUNCTION, returnType.unwrapFlow())
            beginControlFlow(LET_RESULT)
                .addStatement("emit(_result)")
            endControlFlow()
        } else {
            addStatement("emit(%T)", UNIT)
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

    private fun generateSimpleBody(returnType: TypeName, func: FunctionData): CodeBlock = CodeBlock.builder().apply {
        val isUnit = returnType == UNIT
        if (!isUnit) add("return ")
        addRequest(func)
        if (!isUnit) add(".%M()", BODY_FUNCTION)
    }.build()

    private fun CodeBlock.Builder.addRequest(func: FunctionData) = apply {
        beginControlFlow("%M.%M", httpClient, KTOR_REQUEST_FUNCTION)
            .addBuilderCall(func.parameterDataList)
            .addHttpMethod(func)
            .addUrl(func)
            .addHeaders(func)
            .addCookies(func)
            .addBody(func)
            .addAttributes(func.parameterDataList)
        endControlFlow()
    }

    // -- core parts --
    private fun CodeBlock.Builder.addBuilderCall(parameterList: List<ParameterData>) = apply {
        parameterList.firstOrNull { it.isValidTakeFrom }?.let { parameterData ->
            addStatement(
                "this.%L",
                if (parameterData.typeData.typeName == HttpRequestBuilderTypeName) {
                    // member (this.takeFrom)
                    CodeBlock.of("takeFrom(%L)", parameterData.nameString)
                } else {
                    // Extension function
                    CodeBlock.of(
                        MEMBER_LITERAL,
                        KTOR_REQUEST_TAKE_FROM_FUNCTION,
                        parameterData.nameString,
                    )
                },
            )
        }
        parameterList.firstOrNull { it.isHttpRequestBuilderLambda }?.let { param ->
            addStatement("%L(this)", param.nameString)
        }
    }

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
            addQuery(func.parameterDataList)

            // Add fragment like end of the url #header or #function
            add(getFragmentTextBlock(func))
        }

        if (content.isNotEmpty()) {
            beginControlFlow("this.url")
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

        addHeaderParameter(func.parameterDataList)

        addHeaderMap(func.parameterDataList)

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
        addStatement(
            """
        |this.%M(
          |name = %S,
          |value = %P,
          |maxAge = %L,
          |expires = %L,
          |domain = %L,
          |path = %L,
          |secure = %L,
          |httpOnly = %L,
          |extensions = %L,
        |)
            """.trimMargin(),
            KTOR_REQUEST_COOKIE_FUNCTION,
            cookieValues.name,
            if (cookieValues.isValueParameter) {
                if (useVarargItem) $$"$it" else "$${cookieValues.value}"
            } else {
                cookieValues.value
            },
            cookieValues.maxAge,
            cookieValues.expiresTimestamp?.let { timestamp -> CodeBlock.of("%T(%L)", KTOR_GMT_DATE_CLASS, timestamp) },
            cookieValues.domain,
            cookieValues.path,
            cookieValues.secure,
            cookieValues.httpOnly,
            cookieValues.extensions.takeIf { it.isNotEmpty() }?.let { extensions ->
                CodeBlock.builder().apply {
                    add("%M(\n", KOTLIN_MAP_OF)
                    indent()
                    extensions.forEach { (key, value) ->
                        add("%S to %L,\n", key, value?.let { CodeBlock.of("%S", it) })
                    }
                    unindent()
                    add(")")
                }.build()
            } ?: CodeBlock.of("%M()", KOTLIN_EMPTY_MAP),
        )
    }

    private class CookieCandidate(
        val cookieValues: CookieValues,
        val useVarargItem: Boolean = false,
        val varargParamName: String? = null,
    )

    private fun CodeBlock.Builder.addQuery(params: List<ParameterData>) = apply {
        add(getQueryTextBlock(params))
        add(getQueryNameTextBlock(params))
        add(getQueryMapTextBlock(params))
    }

    private fun CodeBlock.Builder.addBody(func: FunctionData) = apply {
        if (func.isBody) {
            addStatement(
                THIS_MEMBER_LITERAL,
                KTOR_REQUEST_SET_BODY_FUNCTION,
                func.parameterDataList.first { it.hasAnnotation<ParameterAnnotation.Body>() }.nameString,
            )
        }
        if (func.isMultipart) add(getPartsCodeBlock(func.parameterDataList))
        if (func.isFormUrl) add(getFieldArgumentsCodeBlock(func.parameterDataList))
    }

    private fun CodeBlock.Builder.addAttributes(parameterDataList: List<ParameterData>) = apply {
        parameterDataList
            .filter { it.hasAnnotation<ParameterAnnotation.Tag>() }
            .forEach { param ->
                val tag = param.findAnnotation<ParameterAnnotation.Tag>()

                if (param.typeData.parameterType.isMarkedNullable) {
                    addStatement(
                        "%N?.let { _value -> this.attributes.put(%T(%S), _value) }",
                        param.nameString,
                        KTOR_ATTRIBUTE_KEY,
                        tag.value,
                    )
                } else {
                    addStatement(
                        "this.attributes.put(%T(%S), %N)",
                        KTOR_ATTRIBUTE_KEY,
                        tag.value,
                        param.nameString,
                    )
                }
            }
    }

    // -- header --
    private fun CodeBlock.Builder.addHeaderParameter(parameterDataList: List<ParameterData>) = apply {
        parameterDataList
            .filter { it.hasAnnotation<ParameterAnnotation.Header>() }
            .takeIf { it.isNotEmpty() }
            ?.also { beginControlFlow(THIS_HEADERS, KTOR_CLIENT_REQUEST_HEADER_FUNCTION) }
            ?.forEach { parameterData ->
                val paramName = parameterData.nameString

                val headers = parameterData.findAllAnnotations<ParameterAnnotation.Header>()
                val starProj = parameterData.typeData.parameterType.starProjection()
                val isList = starProj.isAssignableFrom(listType)
                val isArray = starProj.isAssignableFrom(arrayType)
                val isVararg = parameterData.isVararg

                headers.forEach { headerAnn ->
                    val headerName = headerAnn.value

                    when {
                        isList || isArray -> headerParameterListOrArray(parameterData, paramName, headerName)

                        isVararg -> {
                            // Tratamos el vararg como lista
                            beginControlFlow(LITERAL_FOREACH, paramName)
                                .addStatement(APPEND_STRING_LITERAL, headerName, CodeBlock.of($$"\"$it\""))
                            endControlFlow()
                        }

                        else -> {
                            val headerValue = CodeBlock.of("\"$%L\"", paramName)
                            if (parameterData.typeData.parameterType.isMarkedNullable) {
                                beginControlFlow(LITERAL_NN_LET, paramName)
                                    .addStatement(APPEND_STRING_LITERAL, headerName, headerValue)
                                endControlFlow()
                            } else {
                                addStatement(APPEND_STRING_LITERAL, headerName, headerValue)
                            }
                        }
                    }
                }
            }?.also { endControlFlow() }
    }

    private fun CodeBlock.Builder.headerParameterListOrArray(
        parameterData: ParameterData,
        paramName: String,
        headerName: String,
    ) {
        val typeArg = (parameterData.typeData.typeName as? ParameterizedTypeName)
            ?.typeArguments?.firstOrNull()
        val isStringListOrArray = typeArg?.copy(false)?.equals(STRING) == true
        val hasNullableInnerType = typeArg?.isNullable == true

        val paramAccess = if (parameterData.typeData.parameterType.isMarkedNullable) {
            CodeBlock.of("%L?", paramName)
        } else {
            CodeBlock.of("%L", paramName)
        }

        val listExpr = CodeBlock.builder().add(paramAccess)
        if (hasNullableInnerType) {
            listExpr.add(".filterNotNull()")
            if (parameterData.typeData.parameterType.isMarkedNullable) {
                listExpr.add("?")
            }
        }

        beginControlFlow(LITERAL_FOREACH, listExpr.build())
            .addStatement(
                APPEND_STRING_LITERAL,
                headerName,
                if (isStringListOrArray) CodeBlock.of("it") else CodeBlock.of($$"\"$it\""),
            )
        endControlFlow()
    }

    private fun CodeBlock.Builder.addHeaderMap(parameterDataList: List<ParameterData>) = apply {
        parameterDataList
            .filter { it.hasAnnotation<ParameterAnnotation.HeaderMap>() }
            .takeIf { it.isNotEmpty() }
            ?.also { beginControlFlow(THIS_HEADERS, KTOR_CLIENT_REQUEST_HEADER_FUNCTION) }
            ?.forEach { parameterData ->
                val typeName = parameterData.typeData.typeName
                val paramName = parameterData.nameString

                val isNullable = parameterData.typeData.parameterType.isMarkedNullable

                val isMap = MAP == typeName.rawType()
                val isPair = KOTLIN_PAIR_CLASS == typeName.rawType()
                val isVarargPair = parameterData.isVararg && isPair
                val isVarargMap = parameterData.isVararg && isMap

                val typeArgs = (typeName as? ParameterizedTypeName)?.typeArguments ?: emptyList()
                val valueType = typeArgs.getOrNull(1) ?: typeArgs.getOrNull(0)
                val valueIsString = valueType?.copy(false)?.equals(STRING) == true
                val valueIsNullable = valueType?.isNullable == true

                val paramAccess = if (isNullable) {
                    CodeBlock.of("%L?", paramName)
                } else {
                    CodeBlock.of("%L", paramName)
                }

                when {
                    isMap -> headerMapOrVarargMap(isVarargMap, paramAccess, valueIsString, valueIsNullable)

                    isPair -> headerPairOrVarargPair(isVarargPair, paramName, valueIsString, valueIsNullable)
                }
            }?.also { endControlFlow() }
    }

    private fun TypeName.rawType(): ClassName = when (this) {
        is ParameterizedTypeName -> this.rawType
        is ClassName -> this
        Dynamic,
        is LambdaTypeName,
        is TypeVariableName,
        is WildcardTypeName,
        -> checkImplementation { "Unsupported type: $this" }
    }

    private fun CodeBlock.Builder.headerMapOrVarargMap(
        isVarargMap: Boolean,
        paramAccess: CodeBlock,
        valueIsString: Boolean,
        valueIsNullable: Boolean,
    ) {
        if (isVarargMap) {
            beginControlFlow(LITERAL_FOREACH, paramAccess)
            beginControlFlow("it.forEach")
        } else {
            beginControlFlow(LITERAL_FOREACH, paramAccess)
        }
        if (valueIsNullable) {
            beginControlFlow("it.value?.let { value ->")
        }
        addStatement(
            "this.append(it.key, %L)",
            if (valueIsString) {
                if (valueIsNullable) CodeBlock.of("value") else CodeBlock.of("it.value")
            } else {
                if (valueIsNullable) CodeBlock.of($$"\"$value\"") else CodeBlock.of($$"\"${it.value}\"")
            },
        )
        if (valueIsNullable) endControlFlow()
        endControlFlow() // end inner forEach
        if (isVarargMap) endControlFlow() // end outer forEach
    }

    private fun CodeBlock.Builder.headerPairOrVarargPair(
        isVarargPair: Boolean,
        paramName: String,
        valueIsString: Boolean,
        valueIsNullable: Boolean,
    ) {
        val forEachExpr = if (isVarargPair) {
            CodeBlock.of("%L", paramName)
        } else {
            CodeBlock.of(MEMBER_LITERAL, KOTLIN_LIST_OF, paramName)
        }
        beginControlFlow(LITERAL_FOREACH, forEachExpr)

        if (valueIsNullable) beginControlFlow("it.second?.let { _value ->")

        addStatement(
            "this.append(it.first, %L)",
            if (valueIsString) {
                if (valueIsNullable) CodeBlock.of("_value") else CodeBlock.of("it.second")
            } else {
                if (valueIsNullable) CodeBlock.of($$"\"$_value\"") else CodeBlock.of($$"\"${it.second}\"")
            },
        )

        if (valueIsNullable) endControlFlow()
        endControlFlow()
    }

    // -- query --
    private fun getQueryMapTextBlock(params: List<ParameterData>) = CodeBlock.builder().apply {
        params.filter { it.hasAnnotation<ParameterAnnotation.QueryMap>() }
            .forEach { parameterData ->
                val queryMap = parameterData.findAnnotation<ParameterAnnotation.QueryMap>()
                val encoded = queryMap.encoded
                val data = parameterData.nameString

                beginControlFlow(LITERAL_FOREACH_SAFE_NULL_ENTRY, data)
                beginControlFlow(ENTRY_VALUE_NN_LET)
                    .addStatement(
                        "%L(entry.key, %P)",
                        if (encoded) ENCODED_PARAMETERS_APPEND else PARAMETERS_APPEND,
                        VALUE,
                    )
                endControlFlow()
                endControlFlow()
            }
    }.build()

    private fun getQueryNameTextBlock(params: List<ParameterData>) = CodeBlock.builder().apply {
        params
            .filter { it.hasAnnotation<ParameterAnnotation.QueryName>() }
            .forEach { parameterData ->
                val queryName = parameterData.findAnnotation<ParameterAnnotation.QueryName>()
                val encoded = queryName.encoded
                val name = parameterData.nameString

                val starProj = parameterData.typeData.parameterType.starProjection()
                val isList = starProj.isAssignableFrom(listType)
                val isArray = starProj.isAssignableFrom(arrayType)

                if (isList || isArray) {
                    beginControlFlow(ITERABLE_FILTER_NULL_FOREACH, name)
                        .addStatement(
                            "%L.appendAll(%P, emptyList())",
                            if (encoded) "this.encodedParameters" else "this.parameters",
                            $$"$it",
                        )
                    endControlFlow()
                } else {
                    addStatement(
                        "%L.appendAll(%P, emptyList())",
                        if (encoded) "this.encodedParameters" else "this.parameters",
                        "$$name",
                    )
                }
            }
    }.build()

    private fun getQueryTextBlock(params: List<ParameterData>) = CodeBlock.builder().apply {
        params
            .filter { it.hasAnnotation<ParameterAnnotation.Query>() }
            .forEach { parameterData ->
                val query = parameterData.findAnnotation<ParameterAnnotation.Query>()
                val encoded = query.encoded
                val starProj = parameterData.typeData.parameterType.starProjection()
                val isList = starProj.isAssignableFrom(listType)
                val isArray = starProj.isAssignableFrom(arrayType)

                if (isList || isArray) {
                    beginControlFlow(ITERABLE_FILTER_NULL_FOREACH, parameterData.nameString)
                    addStatement(
                        "%L(%S, %P)",
                        if (encoded) ENCODED_PARAMETERS_APPEND else PARAMETERS_APPEND,
                        query.value,
                        $$"$it",
                    )
                    endControlFlow()
                } else {
                    beginControlFlow(LITERAL_NN_LET, parameterData.nameString)
                    addStatement(
                        "%L(%S, %P)",
                        if (encoded) ENCODED_PARAMETERS_APPEND else PARAMETERS_APPEND,
                        query.value,
                        $$"$it",
                    )
                    endControlFlow()
                }
            }
    }.build()

    private fun getFragmentTextBlock(func: FunctionData) = CodeBlock.builder().apply {
        func.findAnnotationOrNull<FunctionAnnotation.Fragment>()?.let {
            addStatement("this.%L = %S", if (it.encoded) "encodedFragment" else "fragment", it.value)
        }
    }.build()

    // -- body --
    private fun getPartsCodeBlock(params: List<ParameterData>): CodeBlock {
        val partCode = CodeBlock.builder()

        params.filter { it.hasAnnotation<ParameterAnnotation.Part>() }
            .forEach { parameterData ->
                val part = parameterData.findAnnotation<ParameterAnnotation.Part>()
                val name = parameterData.nameString
                val partValue = part.value

                val starProj = parameterData.typeData.parameterType
                val isList = listType.isAssignableFrom(starProj)
                val isArray = arrayType.isAssignableFrom(starProj)
                val isPartData = partDataKtor != null && partDataKtor.isAssignableFrom(starProj)
                val isListPartData = (isList || isArray) &&
                    partDataKtor != null &&
                    starProj.arguments.firstOrNull()?.type?.resolve()?.let { partDataKtor.isAssignableFrom(it) } ==
                    true

                when {
                    isListPartData -> {
                        partCode.beginControlFlow(LITERAL_NN_LET, name)
                            .addStatement("%L.addAll(it)", PART_DATA_LIST_VARIABLE)
                        partCode.endControlFlow()
                    }

                    isPartData -> {
                        partCode.beginControlFlow(LITERAL_NN_LET, name)
                            .addStatement("%L.add(it)", PART_DATA_LIST_VARIABLE)
                        partCode.endControlFlow()
                    }

                    isList || isArray -> {
                        partCode.beginControlFlow(ITERABLE_FILTER_NULL_FOREACH, name)
                            .addStatement(APPEND_STRING_STRING, partValue, $$"$it")
                        partCode.endControlFlow()
                    }

                    else -> {
                        partCode.beginControlFlow(LITERAL_NN_LET, name)
                            .addStatement(APPEND_STRING_STRING, partValue, $$"$it")
                        partCode.endControlFlow()
                    }
                }
            }

        params.filter { it.hasAnnotation<ParameterAnnotation.PartMap>() }
            .forEach { parameterData ->
                val starProj = parameterData.typeData.parameterType
                val isList = starProj.isAssignableFrom(listType)
                val isArray = starProj.isAssignableFrom(arrayType)
                val isListPartData = (isList || isArray) &&
                    partDataKtor != null &&
                    starProj.arguments.firstOrNull()?.type?.resolve()?.let { partDataKtor.isAssignableFrom(it) } ==
                    true

                when {
                    isListPartData -> {
                        partCode.beginControlFlow(LITERAL_NN_LET, parameterData.nameString)
                            .addStatement("%L.addAll(it)", PART_DATA_LIST_VARIABLE)
                        partCode.endControlFlow()
                    }

                    else -> {
                        partCode.beginControlFlow(LITERAL_FOREACH_SAFE_NULL_ENTRY, parameterData.nameString)
                        partCode.beginControlFlow(ENTRY_VALUE_NN_LET)
                            .addStatement("this.append(entry.key, %P)", VALUE)
                        partCode.endControlFlow()
                        partCode.endControlFlow()
                    }
                }
            }

        return CodeBlock.builder().applyIf(partCode.isNotEmpty()) {
            addStatement("val %L = mutableListOf<%T>()", PART_DATA_LIST_VARIABLE, KTOR_PART_DATA_CLASS)
            beginControlFlow("val %L = %M", FORM_DATA_VARIABLE, KTOR_REQUEST_FORM_DATA_FUNCTION)
                .add(partCode.build())
            endControlFlow()

            // Unir contenido de formData y lista de PartData
            addStatement(
                "this.%M(%T(%L + %L))",
                KTOR_REQUEST_SET_BODY_FUNCTION,
                KTOR_REQUEST_MULTIPART_CONTENT_CLASS,
                PART_DATA_LIST_VARIABLE,
                FORM_DATA_VARIABLE,
            )
        }.build()
    }

    private fun getFieldArgumentsCodeBlock(params: List<ParameterData>): CodeBlock {
        val fieldCode = CodeBlock.builder().apply {
            params.filter { it.hasAnnotation<ParameterAnnotation.Field>() }
                .forEach { parameterData ->
                    val field = parameterData.findAnnotation<ParameterAnnotation.Field>()
                    val encoded = field.encoded
                    val paramName = parameterData.nameString
                    val fieldValue = field.value

                    val starProj = parameterData.typeData.parameterType.starProjection()
                    val isList = starProj.isAssignableFrom(listType)
                    val isArray = starProj.isAssignableFrom(arrayType)

                    val controlFlow = if (isList || isArray) ITERABLE_FILTER_NULL_FOREACH else LITERAL_NN_LET

                    beginControlFlow(controlFlow, paramName)
                        .addStatement(
                            "%L",
                            if (encoded) {
                                CodeBlock.of(
                                    "this.append(%S, %P.%M(plusIsSpace = true))",
                                    fieldValue,
                                    $$"$it",
                                    DECODE_URL_COMPONENTS_FUNCTION,
                                )
                            } else {
                                CodeBlock.of(APPEND_STRING_STRING, fieldValue, $$"$it")
                            },
                        )
                    endControlFlow()
                }

            params.filter { it.hasAnnotation<ParameterAnnotation.FieldMap>() }
                .forEach { parameterData ->
                    val encoded = parameterData.findAnnotation<ParameterAnnotation.FieldMap>().encoded

                    beginControlFlow(LITERAL_FOREACH_SAFE_NULL_ENTRY, parameterData.nameString)
                        .beginControlFlow(ENTRY_VALUE_NN_LET)
                        .addStatement(
                            "%L",
                            if (encoded) {
                                CodeBlock.of(
                                    "this.append(entry.key, %P.%M(plusIsSpace = true))",
                                    VALUE,
                                    DECODE_URL_COMPONENTS_FUNCTION,
                                )
                            } else {
                                CodeBlock.of("this.append(entry.key, %P)", VALUE)
                            },
                        )
                        .endControlFlow()
                    endControlFlow()
                }
        }

        return CodeBlock.builder()
            .applyIf(fieldCode.isNotEmpty()) {
                beginControlFlow("val %L = %T.build", FORM_DATA_CONTENT_VARIABLE, KTOR_PARAMETERS_CLASS)
                    .add(fieldCode.build())
                endControlFlow()
                // setBody FormDataContent X
                addStatement(
                    "this.%M(%T(%L))",
                    KTOR_REQUEST_SET_BODY_FUNCTION,
                    KTOR_REQUEST_FORM_DATA_CONTENT_CLASS,
                    FORM_DATA_CONTENT_VARIABLE,
                )
            }.build()
    }
}
