package io.github.kingg22.ktorgen.model

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.KModifier
import io.github.kingg22.ktorgen.model.annotations.FunctionAnnotation

class FunctionData(
    val name: String,
    val returnType: ReturnType,
    val httpMethodAnnotation: FunctionAnnotation.HttpMethodAnnotation,
    val parameterDataList: List<ParameterData>,
    val modifiers: List<KModifier> = emptyList(),
    val nonKtorAnnotations: List<AnnotationSpec>,
    propagateAnnotations: Boolean = true,
    annotationsToPropagate: Set<FunctionAnnotation> = emptySet(),
    optIns: Set<AnnotationSpec> = emptySet(),
    customHeader: String = KTORG_GENERATED_COMMENT,
) : GenOptions(propagateAnnotations, annotationsToPropagate, optIns, customHeader)
