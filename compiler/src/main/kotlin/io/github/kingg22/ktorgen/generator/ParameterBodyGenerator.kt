package io.github.kingg22.ktorgen.generator

import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.*
import io.github.kingg22.ktorgen.KtorGenWithoutCoverage
import io.github.kingg22.ktorgen.applyIf
import io.github.kingg22.ktorgen.checkImplementation
import io.github.kingg22.ktorgen.model.FunctionData
import io.github.kingg22.ktorgen.model.HttpRequestBuilderTypeName
import io.github.kingg22.ktorgen.model.ParameterData
import io.github.kingg22.ktorgen.model.annotations.ParameterAnnotation

/** This class contains the logic for generating the body of [ParameterAnnotation] */
internal class ParameterBodyGenerator(
    private val partDataKtor: KSType?,
    private val listType: KSType,
    private val arrayType: KSType,
) {

    context(blockBuilder: CodeBlock.Builder)
    fun addBuilderCall(parameterList: Sequence<ParameterData>) = blockBuilder.apply {
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

    context(blockBuilder: CodeBlock.Builder)
    fun addAttributes(parameterDataList: Sequence<ParameterData>) = blockBuilder.apply {
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

    context(blockBuilder: CodeBlock.Builder)
    fun addQuery(params: Sequence<ParameterData>) = blockBuilder.apply {
        add(getQueryTextBlock(params))
        add(getQueryNameTextBlock(params))
        add(getQueryMapTextBlock(params))
    }

    context(blockBuilder: CodeBlock.Builder)
    fun addFragmentUrl(func: FunctionData) = blockBuilder.apply {
        func.parameterDataList.singleOrNull { it.hasAnnotation<ParameterAnnotation.Fragment>() }?.let { param ->
            val fragment = param.findAnnotation<ParameterAnnotation.Fragment>()
            val isNullable = param.typeData.parameterType.isMarkedNullable
            if (isNullable) {
                beginControlFlow(LITERAL_NN_LET, param.nameString)
            }
            addStatement(
                "this.%L = %L",
                if (fragment.encoded) "encodedFragment" else "fragment",
                if (isNullable) CodeBlock.of($$"\"$it\"") else CodeBlock.of("\"$${param.nameString}\""),
            )
            if (isNullable) endControlFlow()
        }
    }

    // -- header --
    context(blockBuilder: CodeBlock.Builder)
    fun addHeaderParameter(parameterDataList: Sequence<ParameterData>) = blockBuilder.apply {
        var foundHeader = false
        parameterDataList
            .filter { it.hasAnnotation<ParameterAnnotation.Header>() }
            .forEachIndexed { index, parameterData ->
                if (index == 0) {
                    foundHeader = true
                    beginControlFlow(THIS_HEADERS, KTOR_CLIENT_REQUEST_HEADER_FUNCTION)
                }
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
            }
        if (foundHeader) endControlFlow()
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

    context(blockBuilder: CodeBlock.Builder)
    fun addHeaderMap(parameterDataList: Sequence<ParameterData>) = blockBuilder.apply {
        var foundHeader = false
        parameterDataList
            .filter { it.hasAnnotation<ParameterAnnotation.HeaderMap>() }
            .forEachIndexed { index, parameterData ->
                if (index == 0) {
                    foundHeader = true
                    beginControlFlow(THIS_HEADERS, KTOR_CLIENT_REQUEST_HEADER_FUNCTION)
                }
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
            }
        if (foundHeader) endControlFlow()
    }

    @KtorGenWithoutCoverage // utility function must not throw an exception
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
    private fun getQueryMapTextBlock(params: Sequence<ParameterData>) = CodeBlock.builder().apply {
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

    private fun getQueryNameTextBlock(params: Sequence<ParameterData>) = CodeBlock.builder().apply {
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
                            "%L.appendAll(%P, %M())",
                            if (encoded) "this.encodedParameters" else "this.parameters",
                            $$"$it",
                            KOTLIN_EMPTY_LIST,
                        )
                    endControlFlow()
                } else {
                    addStatement(
                        "%L.appendAll(%P, %M())",
                        if (encoded) "this.encodedParameters" else "this.parameters",
                        "$$name",
                        KOTLIN_EMPTY_LIST,
                    )
                }
            }
    }.build()

    private fun getQueryTextBlock(params: Sequence<ParameterData>) = CodeBlock.builder().apply {
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

    // -- body --
    fun getPartsCodeBlock(params: Sequence<ParameterData>): CodeBlock {
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

    fun getFieldArgumentsCodeBlock(params: Sequence<ParameterData>): CodeBlock {
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

        return CodeBlock.builder().applyIf(fieldCode.isNotEmpty()) {
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
