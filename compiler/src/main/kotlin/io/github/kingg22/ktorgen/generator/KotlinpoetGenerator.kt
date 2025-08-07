package io.github.kingg22.ktorgen.generator

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import io.github.kingg22.ktorgen.DiagnosticTimer
import io.github.kingg22.ktorgen.KtorGenProcessor.Companion.arrayType
import io.github.kingg22.ktorgen.KtorGenProcessor.Companion.listType
import io.github.kingg22.ktorgen.model.ClassData
import io.github.kingg22.ktorgen.model.FunctionData
import io.github.kingg22.ktorgen.model.HttpClientClassName
import io.github.kingg22.ktorgen.model.KTORG_GENERATED_COMMENT
import io.github.kingg22.ktorgen.model.ParameterData
import io.github.kingg22.ktorgen.model.annotations.CookieValues
import io.github.kingg22.ktorgen.model.annotations.FunctionAnnotation
import io.github.kingg22.ktorgen.model.annotations.HttpMethod
import io.github.kingg22.ktorgen.model.annotations.ParameterAnnotation
import io.github.kingg22.ktorgen.model.annotations.removeWhitespace

class KotlinpoetGenerator : KtorGenGenerator {
    override fun generate(classData: ClassData, timer: DiagnosticTimer.DiagnosticSender): FileSpec {
        // class
        timer.start()
        val visibilityModifier = KModifier.valueOf(classData.visibilityModifier.uppercase())
        val classBuilder = TypeSpec.classBuilder(classData.generatedName)
            .addModifiers(visibilityModifier)
            .addSuperinterface(ClassName(classData.packageNameString, classData.interfaceName))
            .addKdoc(classData.customHeader)
            .addAnnotations(classData.annotationsToPropagate.map(AnnotationSpec.Builder::build))
            .addOriginatingKSFile(classData.ksFile)
        timer.addStep("Creating class for ${classData.interfaceName} to ${classData.generatedName}")

        // constructor with properties
        val (constructor, properties, httpClient) =
            generatePrimaryConstructorAndProperties(classData, visibilityModifier)

        // override functions
        val functions = classData.functions.filter { it.goingToGenerate }

        timer.addStep("Generated primary constructor and properties, going to generate ${functions.size} functions")

        // optimize generation if not going to generate util code
        // no have functions to overrides
        // one property is HttpClient, no have properties to override
        val goingToGenerate = functions.isNotEmpty() || properties.size > 1

        if (!goingToGenerate) {
            return KtorGenGenerator.NO_OP.generate(classData, timer.createTask("Dummy code")).also {
                timer.finish()
            }
        }

        // add super interfaces, constructor and properties
        classBuilder
            .addSuperInterfaces(classData, constructor)
            .primaryConstructor(constructor.build())
            .addProperties(properties)

        // override functions
        functions.forEach { function ->
            classBuilder.addFunction(generateFunction(function, httpClient))
            timer.addStep("Generated implementation function for ${function.name}")
        }

        // file
        timer.addStep("Creating file with all")
        val fileBuilder = FileSpec.builder(classData.packageNameString, classData.generatedName)
            .addAnnotation(
                AnnotationSpec.builder(Suppress::class) // suppress annotations
                    .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                    .addMember("%S", "REDUNDANT_VISIBILITY_MODIFIER")
                    .addMember("%S", "unused")
                    .addMember("%S", "UNUSED_IMPORT")
                    .addMember("%S", "warnings")
                    .addMember("%S", "RemoveSingleExpressionStringTemplate")
                    .addMember("%S", "ktlint")
                    .addMember("%S", "detekt:all")
                    .build(),
            )
            .indent("    ") // use 4 spaces https://pinterest.github.io/ktlint/latest/rules/standard/#indentation
            .addFileComment(classData.customFileHeader) // add a header file

        // add imports
        timer.addStep("Processed file, adding required imports")
        classData.imports.forEach { fileBuilder.addImport(it.substringBeforeLast("."), it.substringAfterLast(".")) }

        if (classData.generateTopLevelFunction) {
            val function = generateTopLevelFactoryFunction(
                classNameImpl = ClassName(classData.packageNameString, classData.generatedName),
                interfaceClassName = ClassName(classData.packageNameString, classData.interfaceName),
                constructorParams = constructor.build().parameters,
            )
            fileBuilder.addFunction(
                function
                    .addModifiers(visibilityModifier)
                    .addOriginatingKSFile(classData.ksFile)
                    .build(),
            )
            timer.addStep("Added top level function factory")
        }

        if (classData.generateCompanionExtFunction && classData.haveCompanionObject) {
            val companionName = timer.requireNotNull(
                classData.ksClassDeclaration.declarations
                    .filterIsInstance<KSClassDeclaration>()
                    .firstOrNull { it.isCompanionObject },
                "${classData.interfaceName} don't have companion object",
                classData.ksClassDeclaration,
            ).toClassName()

            val function = generateCompanionExtensionFunction(
                companionClassName = companionName,
                classNameImpl = ClassName(classData.packageNameString, classData.generatedName),
                interfaceClassName = ClassName(classData.packageNameString, classData.interfaceName),
                constructorParams = constructor.build().parameters,
            )

            fileBuilder.addFunction(
                function
                    .addModifiers(visibilityModifier)
                    .addOriginatingKSFile(classData.ksFile)
                    .build(),
            )
            timer.addStep("Added interface companion extension function factory")
        }

        if (classData.generateHttpClientExtension) {
            val function = generateHttpClientExtensionFunction(
                httpClientClassName = HttpClientClassName,
                classNameImpl = ClassName(classData.packageNameString, classData.generatedName),
                interfaceClassName = ClassName(classData.packageNameString, classData.interfaceName),
                constructorParams = constructor.build().parameters,
            )
            fileBuilder.addFunction(
                function
                    .addModifiers(visibilityModifier)
                    .addOriginatingKSFile(classData.ksFile)
                    .build(),
            )
            timer.addStep("Added http client extension function factory")
        }

        // add class to file and build all
        timer.addStep("Finished file generation")
        return fileBuilder.addType(classBuilder.build()).build().also { timer.finish() }
    }

    /** @return FunSpec del constructor, propiedades y nombre de la propiedad httpClient */
    private fun generatePrimaryConstructorAndProperties(
        classData: ClassData,
        visibilityModifier: KModifier,
    ): Triple<FunSpec.Builder, List<PropertySpec>, MemberName> {
        val primaryConstructorBuilder = FunSpec.constructorBuilder().addModifiers(visibilityModifier)
        val propertiesToAdd = mutableListOf<PropertySpec>()
        var httpClientName = "_httpClient"

        // --- Paso 1: HttpClient ---
        classData.httpClientProperty?.let {
            httpClientName = it.simpleName.asString()

            primaryConstructorBuilder.addParameter(httpClientName, HttpClientClassName)
            propertiesToAdd += PropertySpec.builder(httpClientName, HttpClientClassName)
                .addModifiers(KModifier.OVERRIDE)
                .initializer("%L", httpClientName)
                .build()
        } ?: run {
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

    /** This fill the primary constructor and super interfaces */
    private fun TypeSpec.Builder.addSuperInterfaces(classData: ClassData, primaryConstructor: FunSpec.Builder) = apply {
        classData.superClasses
            .forEach { ref ->
                val ksType = ref.resolve()
                val decl = ksType.declaration
                val className = ksType.toClassName()
                val name = decl.simpleName.asString()
                val parameterName = name.replaceFirstChar { it.lowercase() }

                primaryConstructor.addParameter(parameterName, className)
                addSuperinterface(className, CodeBlock.of(parameterName))
            }
    }

    private fun generateFunction(func: FunctionData, httpClientName: MemberName): FunSpec {
        val funBuilder = FunSpec.builder(func.name)
            .addModifiers(func.modifierSet)
            .returns(func.returnTypeData.typeName)
            .addAnnotations(func.nonKtorGenAnnotations)
            .addKdoc(func.customHeader.ifEmpty { KTORG_GENERATED_COMMENT })

        if (func.isSuspend) funBuilder.addModifiers(KModifier.SUSPEND)

        func.parameterDataList.forEach { param ->
            funBuilder.addParameter(
                name = param.nameString,
                type = param.typeData.typeName,
                modifiers = buildList { if (param.isVararg) add(KModifier.VARARG) },
                // TODO add propagate annotations on parameter
            )
        }

        funBuilder.addCode(generateFunctionBody(func, httpClientName))

        return funBuilder.build()
    }

    private fun generateFunctionBody(func: FunctionData, httpClient: MemberName) = CodeBlock.builder().apply {
        if (func.returnTypeData.typeName != UNIT) add("return ")
        addRequest(func, httpClient)
        // End with get body if return type != Unit
        if (func.returnTypeData.typeName != UNIT) add(".body()")
    }.build()

    private fun generateTopLevelFactoryFunction(
        classNameImpl: ClassName,
        interfaceClassName: ClassName,
        constructorParams: List<ParameterSpec>,
    ) = FunSpec.builder(interfaceClassName.simpleName)
        .returns(interfaceClassName)
        .addParameters(constructorParams.map { it.toBuilder(it.name.removePrefix("_")).build() })
        .addStatement(
            RETURN_TYPE_LITERAL,
            classNameImpl,
            constructorParams.joinToString { it.name.removePrefix("_") },
        )

    private fun generateCompanionExtensionFunction(
        companionClassName: ClassName,
        classNameImpl: ClassName,
        interfaceClassName: ClassName,
        constructorParams: List<ParameterSpec>,
    ) = FunSpec.builder("create")
        .receiver(companionClassName)
        .returns(interfaceClassName)
        .addParameters(constructorParams.map { it.toBuilder(it.name.removePrefix("_")).build() })
        .addStatement(
            RETURN_TYPE_LITERAL,
            classNameImpl,
            constructorParams.joinToString { it.name.removePrefix("_") },
        )

    private fun generateHttpClientExtensionFunction(
        httpClientClassName: ClassName,
        classNameImpl: ClassName,
        interfaceClassName: ClassName,
        constructorParams: List<ParameterSpec>,
    ): FunSpec.Builder {
        val paramsExclHttpClient = constructorParams.filter { it.type != httpClientClassName }

        return FunSpec.builder("create${interfaceClassName.simpleName}")
            .receiver(httpClientClassName)
            .returns(interfaceClassName)
            .addParameters(paramsExclHttpClient)
            .addStatement(
                RETURN_TYPE_LITERAL,
                classNameImpl,
                buildString {
                    append("this")
                    if (paramsExclHttpClient.isNotEmpty()) {
                        append(", ")
                        append(paramsExclHttpClient.joinToString { it.name })
                    }
                },
            )
    }

    // -- Complete request is here --
    private fun CodeBlock.Builder.addRequest(func: FunctionData, httpClient: MemberName) = apply {
        // Start with httpClient.request {
        beginControlFlow("this.%M.request", httpClient)
            // First invoke builders
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
        parameterList.firstOrNull { it.isValidTakeFrom }?.let {
            addStatement("this.takeFrom(%L)", it.nameString)
        }
        parameterList.firstOrNull { it.isHttpRequestBuilderLambda }?.let { param ->
            addStatement("%L(this)", param.nameString)
        }
    }

    private fun CodeBlock.Builder.addHttpMethod(func: FunctionData) = apply {
        if (func.httpMethodAnnotation.httpMethod != HttpMethod.Absent) {
            addStatement("this.method = HttpMethod.parse(%S)", func.httpMethodAnnotation.httpMethod.value)
        }
    }

    private fun CodeBlock.Builder.addUrl(func: FunctionData) = apply {
        val content = CodeBlock.builder().apply {
            // Url takeFrom
            func.parameterDataList.firstOrNull { it.hasAnnotation<ParameterAnnotation.Url>() }?.let {
                addStatement("this.takeFrom(%L)", it.nameString)
            }

            // Url template of the http method
            if (func.urlTemplate.isNotEmpty) {
                addStatement(
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
                        }.toTypedArray()
                        template.format(*values)
                    },
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

        if (func.isFormUrl) addStatement("this.contentType(%L)", "ContentType.Application.FormUrlEncoded")

        addHeaderParameter(func.parameterDataList)

        addHeaderMap(func.parameterDataList)

        func.findAnnotationOrNull<FunctionAnnotation.Headers>()?.value?.map { (name, value) ->
            name.removeWhitespace() to value.removeWhitespace()
        }?.takeIf(List<*>::isNotEmpty)?.let {
            beginControlFlow(THIS_HEADERS)
            for ((key, value) in it) {
                addStatement("this.append(%S, %S)", key, value)
            }
            endControlFlow()
        }
    }

    private fun CodeBlock.Builder.addCookies(func: FunctionData) = apply {
        (
            // Cookies en función
            func.ktorGenAnnotations.filterIsInstance<FunctionAnnotation.Cookies>()
                .flatMap { it.value } + // plus
                // Cookies en parámetros (no vararg)
                func.parameterDataList
                    .filterNot { it.isVararg }
                    .flatMap { p ->
                        p.ktorgenAnnotations.filterIsInstance<ParameterAnnotation.Cookies>().map { it.value }
                    }
                    .flatten()
            )
            .forEach { cookieValues ->
                addCookieStatement(cookieValues)
            }

        // Cookies en parámetros (vararg) → foreach
        func.parameterDataList
            .filter { it.isVararg }
            .forEach { p ->
                p.ktorgenAnnotations.filterIsInstance<ParameterAnnotation.Cookies>()
                    .map { it.value }
                    .flatten()
                    .forEach { cookieValues ->
                        beginControlFlow(LITERAL_FOREACH, p.nameString)
                        addCookieStatement(cookieValues, useVarargItem = true)
                        endControlFlow()
                    }
            }
    }

    private fun CodeBlock.Builder.addCookieStatement(cookieValues: CookieValues, useVarargItem: Boolean = false) {
        addStatement(
            """
        |this.cookie(
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
            cookieValues.name,
            if (cookieValues.isValueParameter) {
                if (useVarargItem) "\$it" else "$${cookieValues.value}"
            } else {
                cookieValues.value
            },
            cookieValues.maxAge,
            cookieValues.expiresTimestamp?.let { CodeBlock.of("GMTDate(%L)", it) },
            cookieValues.domain,
            cookieValues.path,
            cookieValues.secure,
            cookieValues.httpOnly,
            cookieValues.extensions.takeIf(Map<*, *>::isNotEmpty)?.let { extensions ->
                CodeBlock.builder().apply {
                    add("%M(\n", MemberName("kotlin.collections", "mapOf"))
                    indent()
                    extensions.forEach { (key, value) ->
                        add("%S to %L,\n", key, value?.let { CodeBlock.of("%S", it) })
                    }
                    unindent()
                    add(")")
                }.build()
            } ?: "emptyMap()",
        )
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
                func.parameterDataList.firstOrNull { it.hasAnnotation<ParameterAnnotation.Body>() }?.nameString
                    ?: error("Not found body"), // imposible after isBody and validations
            )
        }
        if (func.isMultipart) add(getPartsCodeBlock(func.parameterDataList, listType, arrayType))
        if (func.isFormUrl) add(getFieldArgumentsCodeBlock(func.parameterDataList, listType, arrayType))
    }

    private fun CodeBlock.Builder.addAttributes(parameterDataList: List<ParameterData>) = apply {
        parameterDataList
            .filter { it.hasAnnotation<ParameterAnnotation.Tag>() }
            .forEach { param ->
                val tag = param.findAnnotationOrNull<ParameterAnnotation.Tag>()!!

                if (param.typeData.parameterType.isMarkedNullable) {
                    addStatement(
                        "%N?.let { this.attributes.put(AttributeKey(%S), it) }",
                        param.nameString,
                        tag.value,
                    )
                } else {
                    addStatement(
                        "this.attributes.put(AttributeKey(%S), %N)",
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
            .takeIf(List<*>::isNotEmpty)
            ?.also { beginControlFlow(THIS_HEADERS) }
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
                                .addStatement(APPEND_STRING_LITERAL, headerName, CodeBlock.of("\"\$it\""))
                            endControlFlow()
                        }

                        else -> {
                            val headerValue = CodeBlock.of("\"$%L\"", paramName)
                            if (parameterData.typeData.parameterType.isMarkedNullable) {
                                beginControlFlow(LITERAL_NN_LET, paramName)
                                    .addStatement(APPEND_STRING_LITERAL, headerName, headerValue)
                                    .endControlFlow()
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
                if (isStringListOrArray) CodeBlock.of("it") else CodeBlock.of("\"\$it\""),
            )
        endControlFlow()
    }

    private fun CodeBlock.Builder.addHeaderMap(parameterDataList: List<ParameterData>) = apply {
        parameterDataList
            .filter { it.hasAnnotation<ParameterAnnotation.HeaderMap>() }
            .takeIf(List<*>::isNotEmpty)
            ?.also { beginControlFlow(THIS_HEADERS) }
            ?.forEach { parameterData ->
                val typeName = parameterData.typeData.typeName
                val paramName = parameterData.nameString

                val isNullable = parameterData.typeData.parameterType.isMarkedNullable

                val isMap = typeName.copy(false) == MAP
                val isPair = typeName.copy(false) == ClassName("kotlin", "Pair")
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
            "append(it.key, %L)",
            if (valueIsString) {
                if (valueIsNullable) CodeBlock.of("value") else CodeBlock.of("it.value")
            } else {
                if (valueIsNullable) CodeBlock.of("\"\$value\"") else CodeBlock.of("\"\${it.value}\"")
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
            CodeBlock.of("%M(%L)", MemberName("kotlin.collections", "listOf"), paramName)
        }
        beginControlFlow(LITERAL_FOREACH, forEachExpr)

        if (valueIsNullable) beginControlFlow("it.second?.let { value ->")

        addStatement(
            "append(it.first, %L)",
            if (valueIsString) {
                if (valueIsNullable) CodeBlock.of("value") else CodeBlock.of("it.second")
            } else {
                if (valueIsNullable) CodeBlock.of("\"\$value\"") else CodeBlock.of("\"\${it.second}\"")
            },
        )

        if (valueIsNullable) endControlFlow()
        endControlFlow()
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
                block.beginControlFlow(ENTRY_VALUE_NN_LET)
                block.addStatement(
                    "%L(entry.key, %P)",
                    if (encoded) "encodedParameters.append" else "parameter",
                    VALUE,
                )
                block.endControlFlow()
                block.endControlFlow()
            }
        return block.build()
    }

    private fun getQueryNameTextBlock(params: List<ParameterData>, listType: KSType, arrayType: KSType) =
        CodeBlock.builder().apply {
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
                        beginControlFlow(ITERABLE_FILTER_NULL_FOREACH, name)
                        addStatement(
                            "%L.appendAll(%P, emptyList())",
                            if (encoded) "this.encodedParameters" else "this.parameters",
                            "\$it",
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

    private fun getQueryTextBlock(params: List<ParameterData>, listType: KSType, arrayType: KSType) =
        CodeBlock.builder().apply {
            params
                .filter { it.hasAnnotation<ParameterAnnotation.Query>() }
                .forEach { parameterData ->
                    val query = parameterData.findAnnotationOrNull<ParameterAnnotation.Query>()
                        ?: error("Query annotation not found")
                    val encoded = query.encoded
                    val starProj = parameterData.typeData.parameterType.starProjection()
                    val isList = starProj.isAssignableFrom(listType)
                    val isArray = starProj.isAssignableFrom(arrayType)

                    if (isList || isArray) {
                        beginControlFlow(ITERABLE_FILTER_NULL_FOREACH, parameterData.nameString)
                        addStatement(
                            "%L(%S, %P)",
                            if (encoded) "this.encodedParameters.append" else "this.parameters.append",
                            query.value,
                            "\$it",
                        )
                        endControlFlow()
                    } else {
                        beginControlFlow(LITERAL_NN_LET, parameterData.nameString)
                        addStatement(
                            "%L(%S, %P)",
                            if (encoded) "this.encodedParameters.append" else "this.parameters.append",
                            query.value,
                            "\$it",
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

                val starProj = parameterData.typeData.parameterType
                val isList = starProj.isAssignableFrom(listType)
                val isArray = starProj.isAssignableFrom(arrayType)

                if (isList || isArray) {
                    partCode.beginControlFlow(ITERABLE_FILTER_NULL_FOREACH, name)
                    partCode.addStatement("this.append(%S, %P)", partValue, "\$it")
                    partCode.endControlFlow()
                } else {
                    partCode.beginControlFlow(LITERAL_NN_LET, name)
                    partCode.addStatement("this.append(%S, %P)", partValue, "\$it")
                    partCode.endControlFlow()
                }
            }

        params.filter { it.hasAnnotation<ParameterAnnotation.PartMap>() }
            .forEach { parameterData ->
                partCode.beginControlFlow("%L?.forEach", parameterData.nameString)
                partCode.beginControlFlow(ENTRY_VALUE_NN_LET)
                partCode.addStatement("this.append(entry.key, %P)", VALUE)
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

                val starProj = parameterData.typeData.parameterType.starProjection()
                val isList = starProj.isAssignableFrom(listType)
                val isArray = starProj.isAssignableFrom(arrayType)
                val decodeSuffix = if (encoded) ".decodeURLQueryComponent(plusIsSpace = true)" else ""

                if (isList || isArray) {
                    fieldCode.beginControlFlow(ITERABLE_FILTER_NULL_FOREACH, paramName)
                    fieldCode.addStatement("this.append(%S, %P%L)", fieldValue, "\$it", decodeSuffix)
                    fieldCode.endControlFlow()
                } else {
                    fieldCode.beginControlFlow(LITERAL_NN_LET, paramName)
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
                fieldCode.beginControlFlow(ENTRY_VALUE_NN_LET)
                fieldCode.addStatement("this.append(entry.key, %P%L)", VALUE, decodeSuffix)
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

    companion object {
        private const val THIS_HEADERS = "this.headers"
        private const val LITERAL_FOREACH = "%L.forEach"
        private const val APPEND_STRING_LITERAL = "append(%S, %L)"
        private const val LITERAL_NN_LET = "%L?.let"
        private const val ITERABLE_FILTER_NULL_FOREACH = "%L?.filterNotNull()?.forEach"
        private const val ENTRY_VALUE_NN_LET = "entry.value?.let { value ->"
        private const val VALUE = "\$value"
        private const val RETURN_TYPE_LITERAL = "return %T(%L)"
    }
}
