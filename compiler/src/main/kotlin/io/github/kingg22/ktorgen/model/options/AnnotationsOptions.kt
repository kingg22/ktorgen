package io.github.kingg22.ktorgen.model.options

import com.squareup.kotlinpoet.AnnotationSpec

/** Equivalent to [io.github.kingg22.ktorgen.core.KtorGenAnnotationPropagation] */
data class AnnotationsOptions(
    val propagateAnnotations: Boolean,
    val annotations: Set<AnnotationSpec>,
    val optIns: Set<AnnotationSpec>,
    val optInAnnotation: AnnotationSpec?,
    val factoryFunctionAnnotations: Set<AnnotationSpec> = emptySet(),
    override val isDeclaredAtInterface: Boolean,
    override val isDeclaredAtCompanionObject: Boolean,
    override val isDeclaredAtFunctionLevel: Boolean,
) : DeclaredPosition {
    companion object {
        @JvmField
        val NO_ANNOTATIONS = AnnotationsOptions(
            propagateAnnotations = false,
            annotations = emptySet(),
            optIns = emptySet(),
            optInAnnotation = null,
            factoryFunctionAnnotations = emptySet(),
            isDeclaredAtInterface = false,
            isDeclaredAtCompanionObject = false,
            isDeclaredAtFunctionLevel = false,
        )
    }
}
