package io.github.kingg22.ktorgen.model

import com.squareup.kotlinpoet.AnnotationSpec

abstract class Options(
    val generate: Boolean,
    val propagateAnnotations: Boolean,
    val annotations: Set<AnnotationSpec>,
    val optIns: Set<AnnotationSpec>,
    val optInAnnotation: AnnotationSpec?,
) {
    val goingToGenerate = generate
    val annotationsToPropagate = annotations
    override fun toString() =
        "Options(generate=$generate, annotations=$annotations, propagateAnnotations=$propagateAnnotations, optIns=$optIns, optInAnnotation=$optInAnnotation)"
}
