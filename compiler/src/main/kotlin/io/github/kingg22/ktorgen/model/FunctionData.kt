package io.github.kingg22.ktorgen.model

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.KModifier
import io.github.kingg22.ktorgen.model.annotations.FunctionAnnotation
import io.github.kingg22.ktorgen.model.annotations.ParameterAnnotation

class FunctionData(
    val name: String,
    val returnTypeData: TypeData,
    val httpMethodAnnotation: FunctionAnnotation.HttpMethodAnnotation,
    val parameterDataList: List<ParameterData>,
    val isSuspend: Boolean = false,
    val modifierSet: Set<KModifier> = emptySet(),
    val ktorGenAnnotations: List<FunctionAnnotation>,
    val nonKtorGenAnnotations: List<AnnotationSpec>,
    val isImplemented: Boolean = false,
    val ksFunctionDeclaration: KSFunctionDeclaration,
    goingToGenerate: Boolean = ktorGenAnnotations.contains(FunctionAnnotation.Ignore).not(),
    visibilityModifier: String = "public",
    propagateAnnotations: Boolean = true,
    annotationsToPropagate: Set<AnnotationSpec> = emptySet(),
    optIns: Set<AnnotationSpec> = emptySet(),
    customHeader: String = "",
) : GenOptions(
    goingToGenerate = goingToGenerate,
    visibilityModifier = visibilityModifier,
    propagateAnnotations = propagateAnnotations,
    annotationsToPropagate = annotationsToPropagate,
    optIns = optIns,
    customHeader = customHeader,
) {
    val urlTemplate by lazy { parseUrlTemplate(httpMethodAnnotation.path) }
    val isBody by lazy { parameterDataList.any { it.hasAnnotation<ParameterAnnotation.Body>() } }
    val isFormUrl by lazy {
        hasAnnotation<FunctionAnnotation.FormUrlEncoded>() ||
            parameterDataList.any { param ->
                param.hasAnnotation<ParameterAnnotation.Field>() || param.hasAnnotation<ParameterAnnotation.FieldMap>()
            }
    }
    val isMultipart by lazy {
        hasAnnotation<FunctionAnnotation.Multipart>() ||
            parameterDataList.any { param ->
                param.hasAnnotation<ParameterAnnotation.Part>() || param.hasAnnotation<ParameterAnnotation.PartMap>()
            }
    }
    val isBodyInferred by lazy { listOf(isBody, isFormUrl, isMultipart).count { it } == 1 }

    inline fun <reified T : FunctionAnnotation> findAnnotationOrNull(): T? =
        this.ktorGenAnnotations.filterIsInstance<T>().firstOrNull()

    inline fun <reified T : FunctionAnnotation> hasAnnotation() = this.findAnnotationOrNull<T>() != null

    data class UrlTemplateResult(val template: String, val keys: List<String>)

    private fun parseUrlTemplate(url: String): UrlTemplateResult {
        val regex = "\\{([^}]+)}".toRegex()
        val keys = mutableListOf<String>()

        val template = regex.replace(url) { match ->
            val key = match.groupValues[1]
            keys.add(key)
            "%s"
        }

        return UrlTemplateResult(template, keys)
    }
}
