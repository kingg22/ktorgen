package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import io.github.kingg22.ktorgen.DiagnosticSender
import io.github.kingg22.ktorgen.http.*
import io.github.kingg22.ktorgen.model.KTORGEN_DEFAULT_VALUE
import io.github.kingg22.ktorgen.model.KotlinOptInClassName
import io.github.kingg22.ktorgen.model.ParameterData
import io.github.kingg22.ktorgen.model.TypeData
import io.github.kingg22.ktorgen.model.annotations.ParameterAnnotation
import io.github.kingg22.ktorgen.model.annotations.toCookieValues
import io.github.kingg22.ktorgen.work

internal class ParameterMapper : DeclarationParameterMapper {
    context(timer: DiagnosticSender)
    override fun mapToModel(declaration: KSValueParameter): Pair<ParameterData?, List<KSAnnotated>> {
        val paramName = declaration.name?.asString().orEmpty()
        return timer.work {
            val type = declaration.type.resolve()
            if (type.isError) return@work null to listOf(declaration.type)

            val (annotations, optIns, symbols) = extractAnnotationsFiltered(declaration)
            if (symbols.isNotEmpty()) return@work null to symbols

            val optInAnnotation = if (optIns.isNotEmpty()) {
                AnnotationSpec.builder(KotlinOptInClassName)
                    .addMember(
                        optIns.joinToString { "%T::class" },
                        *optIns.map { it.typeName }.toTypedArray(),
                    ).build()
            } else {
                null
            }

            ParameterData(
                nameString = paramName,
                typeData = TypeData(type),
                ksValueParameter = declaration,
                ktorgenAnnotations = collectParameterAnnotations(declaration).also {
                    timer.addStep("Processed annotations")
                }.asSequence(),
                nonKtorgenAnnotations = annotations.asSequence(),
                optInAnnotation = optInAnnotation,
                isHttpRequestBuilderLambda = isHttpRequestBuilderLambda(type).also {
                    timer.addStep("Processed is http builder lambda: $it")
                },
            ) to emptyList()
        }
    }

    context(_: DiagnosticSender)
    private fun collectParameterAnnotations(declaration: KSValueParameter): List<ParameterAnnotation> = buildList {
        declaration.getAnnotation<Path, ParameterAnnotation.Path>(
            manualExtraction = {
                ParameterAnnotation.Path(
                    it.getArgumentValueByName<String>("value")
                        .replaceExact(KTORGEN_DEFAULT_VALUE, declaration.name.safeString()),
                    it.getArgumentValueByName<Boolean>("encoded") ?: false,
                )
            },
        ) {
            ParameterAnnotation.Path(
                it.value.replaceExact(KTORGEN_DEFAULT_VALUE, declaration.name.safeString()),
                it.encoded,
            )
        }?.let { add(it) }

        declaration.getAnnotation<Query, ParameterAnnotation.Query>(
            manualExtraction = {
                ParameterAnnotation.Query(
                    it.getArgumentValueByName<String>("value")
                        .replaceExact(KTORGEN_DEFAULT_VALUE, declaration.name.safeString()),
                    it.getArgumentValueByName<Boolean>("encoded") ?: false,
                )
            },
        ) {
            ParameterAnnotation.Query(
                it.value.replaceExact(KTORGEN_DEFAULT_VALUE, declaration.name.safeString()),
                it.encoded,
            )
        }?.let { add(it) }

        declaration.getAnnotation<QueryName, ParameterAnnotation.QueryName>(
            manualExtraction = {
                ParameterAnnotation.QueryName(it.getArgumentValueByName<Boolean>("encoded") ?: false)
            },
        ) { ParameterAnnotation.QueryName(it.encoded) }?.let { add(it) }

        declaration.getAnnotation<QueryMap, ParameterAnnotation.QueryMap>(
            manualExtraction = {
                ParameterAnnotation.QueryMap(it.getArgumentValueByName<Boolean>("encoded") ?: false)
            },
        ) { ParameterAnnotation.QueryMap(it.encoded) }?.let { add(it) }

        declaration.getAnnotation<Field, ParameterAnnotation.Field>(
            manualExtraction = {
                ParameterAnnotation.Field(
                    it.getArgumentValueByName<String>("value")
                        .replaceExact(KTORGEN_DEFAULT_VALUE, declaration.name.safeString()),
                    it.getArgumentValueByName<Boolean>("encoded") ?: false,
                )
            },
        ) {
            ParameterAnnotation.Field(
                it.value.replaceExact(KTORGEN_DEFAULT_VALUE, declaration.name.safeString()),
                it.encoded,
            )
        }?.let { add(it) }

        declaration.getAnnotation<FieldMap, ParameterAnnotation.FieldMap>(
            manualExtraction = { ParameterAnnotation.FieldMap(it.getArgumentValueByName<Boolean>("encoded") ?: false) },
        ) { ParameterAnnotation.FieldMap(it.encoded) }?.let { add(it) }

        declaration.getAnnotation<Part, ParameterAnnotation.Part>(
            manualExtraction = {
                ParameterAnnotation.Part(
                    it.getArgumentValueByName<String>("value")
                        .replaceExact(KTORGEN_DEFAULT_VALUE, declaration.name.safeString()),
                    it.getArgumentValueByName<String>("encoding") ?: "binary",
                )
            },
        ) {
            ParameterAnnotation.Part(
                it.value.replaceExact(KTORGEN_DEFAULT_VALUE, declaration.name.safeString()),
                it.encoding,
            )
        }?.let { add(it) }

        declaration.getAnnotation<PartMap, ParameterAnnotation.PartMap>(
            manualExtraction = {
                ParameterAnnotation.PartMap(it.getArgumentValueByName<String>("encoding") ?: "binary")
            },
        ) { ParameterAnnotation.PartMap(it.encoding) }?.let { add(it) }

        declaration.getAnnotation<Body, ParameterAnnotation.Body>(
            manualExtraction = { _ -> ParameterAnnotation.Body },
        ) { _ -> ParameterAnnotation.Body }?.let { add(it) }

        declaration.getAllAnnotation<HeaderParam, ParameterAnnotation.Header>(
            manualExtraction = {
                ParameterAnnotation.Header(
                    it.getArgumentValueByName<String>("value") ?: declaration.name.safeString(),
                )
            },
        ) { ParameterAnnotation.Header(it.value) }.forEach { add(it) }

        declaration.getAnnotation<HeaderMap, ParameterAnnotation.HeaderMap>(
            manualExtraction = { _ -> ParameterAnnotation.HeaderMap },
        ) { _ -> ParameterAnnotation.HeaderMap }?.let { add(it) }

        declaration.getAnnotation<Url, ParameterAnnotation.Url>(
            manualExtraction = { _ -> ParameterAnnotation.Url },
        ) { _ -> ParameterAnnotation.Url }?.let { add(it) }

        declaration.getAnnotation<Tag, ParameterAnnotation.Tag>(
            manualExtraction = {
                ParameterAnnotation.Tag(
                    it.getArgumentValueByName<String>("value")
                        .replaceExact(KTORGEN_DEFAULT_VALUE, declaration.name.safeString()),
                )
            },
        ) {
            ParameterAnnotation.Tag(it.value.replaceExact(KTORGEN_DEFAULT_VALUE, declaration.name.safeString()))
        }?.let { add(it) }

        declaration.getAnnotationsByType<Cookie>()
            .map { it.toCookieValues(declaration.name.safeString()) }
            .toList()
            .takeIf { it.isNotEmpty() }
            ?.let { cookies -> add(ParameterAnnotation.Cookies(cookies)) }

        declaration.getAnnotation(manualExtraction = { annotation ->
            ParameterAnnotation.Fragment(annotation.getArgumentValueByName("encoded") ?: false)
        }) { fragment: Fragment ->
            ParameterAnnotation.Fragment(fragment.encoded)
        }?.let { add(it) }
    }

    private fun isHttpRequestBuilderLambda(type: KSType): Boolean {
        val decl = type.declaration
        if (decl.qualifiedName?.asString() != "kotlin.Function1") return false

        val args = type.arguments
        if (args.size != 2) return false

        val receiverType = args[0].type?.resolve()?.declaration?.qualifiedName?.asString()
        val returnType = args[1].type?.resolve()?.declaration?.qualifiedName?.asString()

        return receiverType == "io.ktor.client.request.HttpRequestBuilder" && returnType == "kotlin.Unit"
    }

    /** Avoid NPE when don't have string representation */
    private fun KSName?.safeString() = this?.asString() ?: ""
}
