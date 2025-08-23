package io.github.kingg22.ktorgen.model

import com.squareup.kotlinpoet.AnnotationSpec

open class FunctionGenerationOptions(
    generate: Boolean,
    propagateAnnotations: Boolean,
    annotationsToPropagate: Set<AnnotationSpec>,
    optIns: Set<AnnotationSpec>,
    val customHeader: String,
    optInAnnotation: AnnotationSpec? = null,
) : Options(
    generate = generate,
    propagateAnnotations = propagateAnnotations,
    annotations = annotationsToPropagate,
    optIns = optIns,
    optInAnnotation = optInAnnotation,
) {
    constructor(options: FunctionGenerationOptions) : this(
        generate = options.generate,
        propagateAnnotations = options.propagateAnnotations,
        annotationsToPropagate = options.annotationsToPropagate,
        optIns = options.optIns,
        optInAnnotation = options.optInAnnotation,
        customHeader = options.customHeader,
    )

    fun copy(block: (FunctionGenerationOptions) -> FunctionGenerationOptions) = block(this)

    fun copy(
        generate: Boolean = this.generate,
        propagateAnnotations: Boolean = this.propagateAnnotations,
        annotations: Set<AnnotationSpec> = this.annotationsToPropagate,
        optIns: Set<AnnotationSpec> = this.optIns,
        optInAnnotation: AnnotationSpec? = this.optInAnnotation,
        customHeader: String = this.customHeader,
    ) = FunctionGenerationOptions(
        generate = generate,
        propagateAnnotations = propagateAnnotations,
        annotationsToPropagate = annotations,
        optIns = optIns,
        optInAnnotation = optInAnnotation,
        customHeader = customHeader,
    )

    companion object {
        val DEFAULT = FunctionGenerationOptions(
            generate = true,
            propagateAnnotations = true,
            annotationsToPropagate = emptySet(),
            optIns = emptySet(),
            optInAnnotation = null,
            customHeader = "",
        )
    }
}
