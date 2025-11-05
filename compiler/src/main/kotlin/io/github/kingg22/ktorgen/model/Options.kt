package io.github.kingg22.ktorgen.model

import com.squareup.kotlinpoet.AnnotationSpec

abstract class Options(
    val goingToGenerate: Boolean,
    val propagateAnnotations: Boolean,
    val annotations: Set<AnnotationSpec>,
    val optIns: Set<AnnotationSpec>,
    val optInAnnotation: AnnotationSpec?,
) {
    val annotationsToPropagate
        inline get() = annotations

    override fun toString() =
        "Options(generate=$goingToGenerate, annotations=$annotations, propagateAnnotations=$propagateAnnotations, optIns=$optIns, optInAnnotation=$optInAnnotation)"
}
