package io.github.kingg22.ktorgen.model

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.KModifier
import io.github.kingg22.ktorgen.model.annotations.FunctionAnnotation

class FunctionData(
    val name: String,
    val returnType: ReturnType,
    val httpMethodAnnotation: FunctionAnnotation.HttpMethodAnnotation,
    val parameterDataList: List<ParameterData>,
    val isSuspend: Boolean = false,
    val modifiers: List<KModifier> = emptyList(),
    val ktorGenAnnotations: List<FunctionAnnotation>,
    val nonKtorGenAnnotations: List<AnnotationSpec>,
    val isImplemented: Boolean = false,
    goingToGenerate: Boolean = true,
    visibilityModifier: String = "public",
    propagateAnnotations: Boolean = true,
    annotationsToPropagate: Set<AnnotationSpec> = emptySet(),
    optIns: Set<AnnotationSpec> = emptySet(),
    customHeader: String = KTORG_GENERATED_COMMENT,
) : GenOptions(
    goingToGenerate = goingToGenerate,
    visibilityModifier = visibilityModifier,
    propagateAnnotations = propagateAnnotations,
    annotationsToPropagate = annotationsToPropagate,
    optIns = optIns,
    customHeader = customHeader,
)
