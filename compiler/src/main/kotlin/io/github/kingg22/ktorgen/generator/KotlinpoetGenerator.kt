package io.github.kingg22.ktorgen.generator

import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import io.github.kingg22.ktorgen.KtorGenProcessor.Companion.arrayType
import io.github.kingg22.ktorgen.KtorGenProcessor.Companion.listType
import io.github.kingg22.ktorgen.model.ClassData
import io.github.kingg22.ktorgen.model.FunctionData
import io.github.kingg22.ktorgen.model.HttpClientClassName
import io.github.kingg22.ktorgen.model.ParameterData
import io.github.kingg22.ktorgen.model.annotations.FunctionAnnotation
import io.github.kingg22.ktorgen.model.annotations.ParameterAnnotation

class KotlinpoetGenerator : KtorGenGenerator {
    override fun generate(classData: ClassData): FileSpec {
        // class
        val classBuilder = TypeSpec.classBuilder(classData.generatedName)
            .addModifiers(KModifier.PUBLIC) // TODO add visibility of user want
            .addSuperinterface(ClassName(classData.packageNameString, classData.interfaceName))
            .addKdoc(classData.customHeader)
            .addOriginatingKSFile(classData.ksFile)

        // constructor with properties
        val (constructor, properties, httpClient) =
            generatePrimaryConstructorAndProperties(classData)
        classBuilder
            .addSuperInterfacesAndConstructor(classData, constructor)
            .addProperties(properties)

        // override functions
        classData.functions.forEach { function -> classBuilder.addFunction(generateFunction(function, httpClient)) }

        // file
        val fileBuilder = FileSpec.builder(classData.packageNameString, classData.generatedName)
            .addAnnotation(
                AnnotationSpec.builder(Suppress::class) // suppress annotations
                    .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                    .addMember("%S", "REDUNDANT_VISIBILITY_MODIFIER")
                    .addMember("%S", "unused")
                    .addMember("%S", "UNUSED_IMPORT")
                    .addMember("%S", "warnings")
                    .addMember("%S", "RemoveSingleExpressionStringTemplate")
                    .build(),
            )
            .addFileComment(classData.customHeader) // add a header file

        // add imports
        classData.imports.forEach { fileBuilder.addImport(it.substringBeforeLast("."), it.substringAfterLast(".")) }

        // add class to file and build all
        return fileBuilder.addType(classBuilder.build()).build()
    }

    /** @return FunSpec del constructor, propiedades y nombre de la propiedad httpClient */
    private fun generatePrimaryConstructorAndProperties(
        classData: ClassData,
    ): Triple<FunSpec.Builder, List<PropertySpec>, MemberName> {
        val primaryConstructorBuilder = FunSpec.constructorBuilder().addModifiers(KModifier.PUBLIC)
        val propertiesToAdd = mutableListOf<PropertySpec>()
        var httpClientName = "_httpClient"

        // --- Paso 1: HttpClient ---
        if (classData.haveHttpClientProperty) {
            val httpProp = classData.properties.first { it.type.toTypeName() == HttpClientClassName }
            httpClientName = httpProp.simpleName.asString()

            primaryConstructorBuilder.addParameter(httpClientName, HttpClientClassName)
            propertiesToAdd += PropertySpec.builder(httpClientName, HttpClientClassName)
                .addModifiers(KModifier.OVERRIDE)
                .initializer("%L", httpClientName)
                .build()
        } else {
            primaryConstructorBuilder.addParameter(httpClientName, HttpClientClassName)
            propertiesToAdd += PropertySpec.builder(httpClientName, HttpClientClassName)
                .addModifiers(KModifier.PRIVATE)
                .initializer("%L", httpClientName)
                .build()
        }

        // --- Paso 2: Resto de propiedades ---
        classData.properties
            .filter { it.type.toTypeName() != HttpClientClassName } // evitar duplicar el HttpClient
            .forEach { property ->
                val paramName = property.simpleName.asString()
                val typeName = property.type.toTypeName()

                // Agregamos parámetro al constructor
                primaryConstructorBuilder.addParameter(paramName, typeName)

                // Creamos la propiedad override enlazada al parámetro
                propertiesToAdd += PropertySpec.builder(paramName, typeName)
                    .addModifiers(KModifier.OVERRIDE)
                    .initializer("%L", paramName)
                    .mutable(property.isMutable)
                    .build()
            }

        return Triple(
            primaryConstructorBuilder,
            propertiesToAdd,
            MemberName(classData.packageNameString, httpClientName),
        )
    }

    private fun TypeSpec.Builder.addSuperInterfacesAndConstructor(
        classData: ClassData,
        primaryConstructor: FunSpec.Builder,
    ) = apply {
        classData.superClasses
            .filterNot { ref -> ref.annotations.any { it.shortName.getShortName() == "KtorGenIgnore" } }
            .forEach { ref ->
                val ksType = ref.resolve()
                val decl = ksType.declaration
                val className = ksType.toClassName()
                val name = decl.simpleName.asString()
                val parameterName = name.replaceFirstChar { it.lowercase() }

                primaryConstructor.addParameter(parameterName, className)
                addSuperinterface(className, CodeBlock.of(parameterName))
            }
        primaryConstructor(primaryConstructor.build())
    }

    private fun generateFunction(func: FunctionData, httpClientName: MemberName): FunSpec {
        val funBuilder = FunSpec.builder(func.name)
            .addModifiers(func.modifierSet)
            .returns(func.returnTypeData.typeName)
            .addAnnotations(func.nonKtorGenAnnotations)
            .addKdoc(func.customHeader)

        if (func.isSuspend) funBuilder.addModifiers(KModifier.SUSPEND)

        func.parameterDataList.forEach { param ->
            funBuilder.addParameter(
                name = param.nameString,
                type = param.typeData.typeName,
                modifiers = buildList { if (param.isVararg) add(KModifier.VARARG) },
                // TODO add propagate annotations
            )
        }

        funBuilder.addCode(generateFunctionBody(func, httpClientName))

        return funBuilder.build()
    }

    private fun generateFunctionBody(func: FunctionData, httpClient: MemberName) = CodeBlock.builder().apply {
        if (func.returnTypeData.typeName != UNIT) add("return ")
        // Start with httpClient.request {
        beginControlFlow("this.%M.request", httpClient)
            // First invoke builders
            .addBuilderCall(func.parameterDataList)
            .addStatement("this.method = HttpMethod.parse(%S)", func.httpMethodAnnotation.httpMethod.value)
            .addUrl(func)
            .addHeaders(func)
            .addBody(func)
            .addAttributes(func.parameterDataList)
        endControlFlow()
        // } End with get body if return type != Unit
        if (func.returnTypeData.typeName != UNIT) add(".body()")
    }.build()

    // -- core parts --
    private fun CodeBlock.Builder.addBuilderCall(parameterList: List<ParameterData>) = this.apply {
        parameterList.firstOrNull { it.isHttpRequestBuilder }?.let {
            addStatement("this.takeFrom(%L)", it.nameString)
        }
        parameterList.firstOrNull { it.isHttpRequestBuilderLambda }?.let { param ->
            addStatement("%L(this)", param.nameString)
        }
    }

    private fun CodeBlock.Builder.addUrl(func: FunctionData) = this.apply {
        beginControlFlow("this.url")
            .apply {
                if (func.parameterDataList.any { it.hasAnnotation<ParameterAnnotation.Url>() }) {
                    addStatement(
                        "this.takeFrom(%L)",
                        func.parameterDataList.first { it.hasAnnotation<ParameterAnnotation.Url>() }.nameString,
                    )
                }
            }.addStatement(
                "this.takeFrom(%P)",
                func.urlTemplate.let { (template, keys) ->
                    val values = keys.map { key ->
                        val param = func.parameterDataList
                            .firstOrNull { it.findAnnotationOrNull<ParameterAnnotation.Path>()?.value == key }
                            ?: error("Missing @Path parameter for {$key}") // after validation is never throw

                        val (path, encoded) = param.findAnnotationOrNull<ParameterAnnotation.Path>()!!
                        if (encoded) {
                            CodeBlock.of("$%L", path).toString() // ${id}
                        } else {
                            CodeBlock.of("\${\"$%L\".encodeURLPath()}", path).toString() // ${"$id".encodeURLPath()}
                        }
                    }
                    template.format(*values.toTypedArray())
                },
            )
            .addQuery(func.parameterDataList)
        endControlFlow()
    }

    private fun CodeBlock.Builder.addHeaders(func: FunctionData) = apply {
        // multipart Ktor handle content type

        if (func.isFormUrl) addStatement("this.contentType(%L)", "ContentType.Application.FormUrlEncoded")

        if (func.parameterDataList.any { it.hasAnnotation<ParameterAnnotation.Header>() }) {
            addHeaderFuncAnnotation(func.parameterDataList)
        }

        if (func.parameterDataList.any { it.hasAnnotation<ParameterAnnotation.HeaderMap>() }) {
            addHeaderMap(func.parameterDataList)
        }

        if (func.hasAnnotation<FunctionAnnotation.Headers>()) {
            func.findAnnotationOrNull<FunctionAnnotation.Headers>()?.value?.map {
                it.replace("\\s+".toRegex(), "")
            }?.toSet()?.associate {
                val (name, value) = it.split(":", limit = 2)
                name.trim() to value.trim()
            }?.let {
                beginControlFlow("this.headers")
                for ((key, value) in it) {
                    addStatement("this.append(%S, %S)", key, value)
                }
                endControlFlow()
            }
        }
    }

    private fun CodeBlock.Builder.addQuery(params: List<ParameterData>) = apply {
        add(getQueryTextBlock(params, listType, arrayType))
        add(getQueryNameTextBlock(params, listType, arrayType))
        add(getQueryMapTextBlock(params))
    }

    private fun CodeBlock.Builder.addBody(func: FunctionData) = apply {
        if (func.isBody) {
            addStatement(
                "this.setBody(%L)",
                func.parameterDataList.first { it.hasAnnotation<ParameterAnnotation.Body>() }.nameString,
            )
        }
        if (func.isMultipart) add(getPartsCodeBlock(func.parameterDataList, listType, arrayType))
        if (func.isFormUrl) add(getFieldArgumentsCodeBlock(func.parameterDataList, listType, arrayType))
    }

    private fun CodeBlock.Builder.addAttributes(parameterDataList: List<ParameterData>) = apply {
        val builder = this

        parameterDataList
            .filter { it.hasAnnotation<ParameterAnnotation.Tag>() }
            .forEach { param ->
                val tag = param.findAnnotationOrNull<ParameterAnnotation.Tag>()!!

                if (param.typeData.parameterType.isMarkedNullable) {
                    builder.addStatement(
                        "%N?.let { this.attributes.put(AttributeKey(%S), it) }",
                        param.nameString,
                        tag.value,
                    )
                } else {
                    builder.addStatement(
                        "this.attributes.put(AttributeKey(%S), %N)",
                        tag.value,
                        param.nameString,
                    )
                }
            }
    }

    // -- header --
    private fun CodeBlock.Builder.addHeaderFuncAnnotation(parameterDataList: List<ParameterData>) = apply {
        val headerCode = this
        beginControlFlow("this.headers")

        parameterDataList
            .filter { it.hasAnnotation<ParameterAnnotation.Header>() }
            .forEach { parameterData ->
                val paramName = parameterData.nameString
                val headerName = parameterData.findAnnotationOrNull<ParameterAnnotation.Header>()?.value.orEmpty()

                val starProj = parameterData.typeData.parameterType?.starProjection()
                val isList = starProj?.isAssignableFrom(listType) ?: false
                val isArray = starProj?.isAssignableFrom(arrayType) ?: false
                val isVararg = parameterData.isVararg

                when {
                    isList || isArray -> {
                        val typeArg = (parameterData.typeData.typeName as? ParameterizedTypeName)
                            ?.typeArguments?.firstOrNull()
                        val isStringListOrArray = typeArg?.toString()?.removeSuffix("?") == "kotlin.String"
                        val hasNullableInnerType = typeArg?.isNullable == true

                        // Bloque para listas/arrays
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

                        headerCode.beginControlFlow("%L.forEach", listExpr.build())
                            .addStatement(
                                "append(%S, %L)",
                                headerName,
                                if (isStringListOrArray) CodeBlock.of("it") else CodeBlock.of("\"\$it\""),
                            )
                            .endControlFlow()
                    }

                    isVararg -> {}

                    else -> {
                        val headerValue = CodeBlock.of("\"$%L\"", paramName)

                        if (parameterData.typeData.parameterType.isMarkedNullable) {
                            headerCode.beginControlFlow("%L?.let", paramName)
                                .addStatement("append(%S, %L)", headerName, headerValue)
                                .endControlFlow()
                        } else {
                            headerCode.addStatement("append(%S, %L)", headerName, headerValue)
                        }
                    }
                }
            }
        endControlFlow()
    }

    private fun CodeBlock.Builder.addHeaderMap(parameterDataList: List<ParameterData>) = apply {
        val headerMapCode = this

        parameterDataList
            .filter { it.hasAnnotation<ParameterAnnotation.HeaderMap>() }
            .forEach { parameterData ->
                val typeName = parameterData.typeData.typeName
                val paramName = parameterData.nameString

                val isNullable = parameterData.typeData.parameterType.isMarkedNullable

                val isMap = typeName.toString().startsWith("kotlin.collections.Map")
                val isPair = typeName.toString().startsWith("kotlin.Pair")
                val isVarargPair = parameterData.isVararg && typeName.toString().startsWith("kotlin.Pair")
                val isVarargMap = parameterData.isVararg && typeName.toString().startsWith("kotlin.collections.Map")

                val typeArgs = (typeName as? ParameterizedTypeName)?.typeArguments ?: emptyList()
                val valueType = typeArgs.getOrNull(1) ?: typeArgs.getOrNull(0)
                val valueIsString = valueType?.toString()?.removeSuffix("?") == "kotlin.String"
                val valueIsNullable = valueType?.isNullable == true

                val paramAccess = if (isNullable) {
                    CodeBlock.of("%L?", paramName)
                } else {
                    CodeBlock.of("%L", paramName)
                }

                when {
                    isMap -> {
                        headerMapCode.beginControlFlow("%L.forEach", paramAccess)
                        if (valueIsNullable) {
                            headerMapCode.beginControlFlow("it.value?.let { value ->")
                        }
                        headerMapCode.addStatement(
                            "append(it.key, %L)",
                            if (valueIsString) {
                                if (valueIsNullable) CodeBlock.of("value") else CodeBlock.of("it.value")
                            } else {
                                if (valueIsNullable) CodeBlock.of("\"\$value\"") else CodeBlock.of("\"\${it.value}\"")
                            },
                        )
                        if (valueIsNullable) headerMapCode.endControlFlow()
                        headerMapCode.endControlFlow()
                    }

                    isVarargMap -> {
                        headerMapCode.beginControlFlow("%L.asIterable().forEach", paramAccess)
                        headerMapCode.beginControlFlow("it.forEach")
                        if (valueIsNullable) {
                            headerMapCode.beginControlFlow("it.value?.let { value ->")
                        }
                        headerMapCode.addStatement(
                            "append(it.key, %L)",
                            if (valueIsString) {
                                if (valueIsNullable) CodeBlock.of("value") else CodeBlock.of("it.value")
                            } else {
                                if (valueIsNullable) CodeBlock.of("\"\$value\"") else CodeBlock.of("\"\${it.value}\"")
                            },
                        )
                        if (valueIsNullable) headerMapCode.endControlFlow()
                        headerMapCode.endControlFlow() // end inner forEach
                        headerMapCode.endControlFlow() // end outer forEach
                    }

                    isPair || isVarargPair -> {
                        val forEachExpr = if (isVarargPair) {
                            CodeBlock.of("%L.asIterable()", paramName)
                        } else {
                            CodeBlock.of("listOf(%L)", paramName)
                        }
                        headerMapCode.beginControlFlow("%L.forEach", forEachExpr)
                        if (valueIsNullable) {
                            headerMapCode.beginControlFlow("it.second?.let { value ->")
                        }
                        headerMapCode.addStatement(
                            "append(it.first, %L)",
                            if (valueIsString) {
                                if (valueIsNullable) CodeBlock.of("value") else CodeBlock.of("it.second")
                            } else {
                                if (valueIsNullable) CodeBlock.of("\"\$value\"") else CodeBlock.of("\"\${it.second}\"")
                            },
                        )
                        if (valueIsNullable) headerMapCode.endControlFlow()
                        headerMapCode.endControlFlow()
                    }
                }
            }
    }

    // -- query --
    private fun getQueryMapTextBlock(params: List<ParameterData>): CodeBlock {
        val block = CodeBlock.builder()
        params
            .filter { it.hasAnnotation<ParameterAnnotation.QueryMap>() }
            .forEach { parameterData ->
                val queryMap = parameterData.findAnnotationOrNull<ParameterAnnotation.QueryMap>()
                    ?: error("QueryMap annotation not found")
                val encoded = queryMap.encoded
                val data = parameterData.nameString

                block.beginControlFlow("%L?.forEach { entry ->", data)
                block.beginControlFlow("entry.value?.let { value ->")
                block.addStatement(
                    "%L(entry.key, %P)",
                    if (encoded) "encodedParameters.append" else "parameter",
                    "\$value",
                )
                block.endControlFlow()
                block.endControlFlow()
            }
        return block.build()
    }

    private fun getQueryNameTextBlock(params: List<ParameterData>, listType: KSType, arrayType: KSType): CodeBlock {
        val block = CodeBlock.builder()
        params
            .filter { it.hasAnnotation<ParameterAnnotation.QueryName>() }
            .forEach { parameterData ->
                val queryName = parameterData.findAnnotationOrNull<ParameterAnnotation.QueryName>()
                    ?: error("QueryName annotation not found")
                val encoded = queryName.encoded
                val name = parameterData.nameString

                val starProj = parameterData.typeData.parameterType.starProjection()
                val isList = starProj.isAssignableFrom(listType)
                val isArray = starProj.isAssignableFrom(arrayType)

                if (isList || isArray) {
                    block.beginControlFlow("%L?.filterNotNull()?.forEach", name)
                    block.addStatement(
                        "%L.appendAll(%P, emptyList())",
                        if (encoded) "this.encodedParameters" else "this.parameters",
                        "\$it",
                    )
                    block.endControlFlow()
                } else {
                    block.addStatement(
                        "%L.appendAll(%P, emptyList())",
                        if (encoded) "this.encodedParameters" else "this.parameters",
                        "\$$name",
                    )
                }
            }
        return block.build()
    }

    private fun getQueryTextBlock(params: List<ParameterData>, listType: KSType, arrayType: KSType): CodeBlock {
        val block = CodeBlock.builder()
        params
            .filter { it.hasAnnotation<ParameterAnnotation.Query>() }
            .forEach { parameterData ->
                val query = parameterData.findAnnotationOrNull<ParameterAnnotation.Query>()
                    ?: error("Query annotation not found")
                val encoded = query.encoded
                val starProj = parameterData.typeData.parameterType?.starProjection()
                val isList = starProj?.isAssignableFrom(listType) ?: false
                val isArray = starProj?.isAssignableFrom(arrayType) ?: false

                if (isList || isArray) {
                    block.beginControlFlow("%L?.filterNotNull()?.forEach", parameterData.nameString)
                    block.addStatement(
                        "%L(%S, %P)",
                        if (encoded) "this.encodedParameters.append" else "this.parameters.append",
                        query.value,
                        if (encoded) "\${\"\$it\".decodeURLQueryComponent()}" else "\$it",
                    )
                    block.endControlFlow()
                } else {
                    block.beginControlFlow("%L?.let", parameterData.nameString)
                    block.addStatement(
                        "%L(%S, %P)",
                        if (encoded) "this.encodedParameters.append" else "this.parameters.append",
                        query.value,
                        if (encoded) "\${\"\$it\".decodeURLQueryComponent()}" else "\$it",
                    )
                    block.endControlFlow()
                }
            }
        return block.build()
    }

    // -- body --
    fun getPartsCodeBlock(
        params: List<ParameterData>,
        listType: KSType,
        arrayType: KSType,
        formDataVar: String = "_multiPartDataContent",
    ): CodeBlock {
        val block = CodeBlock.builder()

        val partCode = CodeBlock.builder()
        params.filter { it.hasAnnotation<ParameterAnnotation.Part>() }
            .forEach { parameterData ->
                val part = parameterData.findAnnotationOrNull<ParameterAnnotation.Part>()
                    ?: error("Part annotation not found")
                val name = parameterData.nameString
                val partValue = part.value

                val starProj = parameterData.typeData.parameterType?.starProjection()
                val isList = starProj?.isAssignableFrom(listType) ?: false
                val isArray = starProj?.isAssignableFrom(arrayType) ?: false

                if (isList || isArray) {
                    partCode.beginControlFlow("%L?.filterNotNull()?.forEach", name)
                    partCode.addStatement("this.append(%S, %P)", partValue, "\$it")
                    partCode.endControlFlow()
                } else {
                    partCode.beginControlFlow("%L?.let", name)
                    partCode.addStatement("this.append(%S, %P)", partValue, "\$it")
                    partCode.endControlFlow()
                }
            }

        params.filter { it.hasAnnotation<ParameterAnnotation.PartMap>() }
            .forEach { parameterData ->
                partCode.beginControlFlow("%L?.forEach", parameterData.nameString)
                partCode.beginControlFlow("entry.value?.let { value ->")
                partCode.addStatement("this.append(entry.key, %P)", "\$value")
                partCode.endControlFlow()
                partCode.endControlFlow()
            }

        if (partCode.build().isNotEmpty()) {
            block.add("val %L = formData·{\n", formDataVar)
            block.indent()
            block.add(partCode.build())
            block.unindent()
            block.add("}\n")
            block.addStatement("this.setBody(MultiPartFormDataContent(%L))", formDataVar)
        }

        return block.build()
    }

    fun getFieldArgumentsCodeBlock(
        params: List<ParameterData>,
        listType: KSType,
        arrayType: KSType,
        formParamsVar: String = "_formDataContent",
    ): CodeBlock {
        val block = CodeBlock.builder()

        val fieldCode = CodeBlock.builder()
        params.filter { it.hasAnnotation<ParameterAnnotation.Field>() }
            .forEach { parameterData ->
                val field = parameterData.findAnnotationOrNull<ParameterAnnotation.Field>()
                    ?: error("Field annotation not found")
                val encoded = field.encoded
                val paramName = parameterData.nameString
                val fieldValue = field.value

                val starProj = parameterData.typeData.parameterType?.starProjection()
                val isList = starProj?.isAssignableFrom(listType) ?: false
                val isArray = starProj?.isAssignableFrom(arrayType) ?: false
                val decodeSuffix = if (encoded) ".decodeURLQueryComponent(plusIsSpace = true)" else ""

                if (isList || isArray) {
                    fieldCode.beginControlFlow("%L?.filterNotNull()?.forEach", paramName)
                    fieldCode.addStatement("this.append(%S, %P%L)", fieldValue, "\$it", decodeSuffix)
                    fieldCode.endControlFlow()
                } else {
                    fieldCode.beginControlFlow("%L?.let", paramName)
                    fieldCode.addStatement("this.append(%S, %P%L)", fieldValue, "\$it", decodeSuffix)
                    fieldCode.endControlFlow()
                }
            }

        params.filter { it.hasAnnotation<ParameterAnnotation.FieldMap>() }
            .forEach { parameterData ->
                val fieldMap = parameterData.findAnnotationOrNull<ParameterAnnotation.FieldMap>()
                    ?: error("FieldMap annotation not found")
                val encoded = fieldMap.encoded
                val decodeSuffix = if (encoded) ".decodeURLQueryComponent(plusIsSpace = true)" else ""

                fieldCode.beginControlFlow("%L?.forEach", parameterData.nameString)
                fieldCode.beginControlFlow("entry.value?.let { value ->")
                fieldCode.addStatement("this.append(entry.key, %P%L)", "\$value", decodeSuffix)
                fieldCode.endControlFlow()
                fieldCode.endControlFlow()
            }

        if (fieldCode.build().isNotEmpty()) {
            block.add("val %L = Parameters.build·{\n", formParamsVar)
            block.indent()
            block.add(fieldCode.build())
            block.unindent()
            block.add("}\n")
            block.addStatement("this.setBody(FormDataContent(%L))", formParamsVar)
        }

        return block.build()
    }
}
