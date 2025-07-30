package io.github.kingg22.ktorgen.model

import com.squareup.kotlinpoet.AnnotationSpec
import io.github.kingg22.ktorgen.model.annotations.FunctionAnnotation

open class GenOptions(
    val propagateAnnotations: Boolean = true,
    val annotationsToPropagate: Set<FunctionAnnotation> = emptySet(),
    val optIns: Set<AnnotationSpec> = emptySet(),
    val customHeader: String = KTORG_GENERATED_COMMENT,
) {
    /** Options on Interface or Companion Object */
    open class GenTypeOption(
        val generatedName: String,
        val generateTopLevelFunction: Boolean = true,
        val generateCompanionFunction: Boolean = false,
        val generateExtensions: Boolean = false,
        val jvmStatic: Boolean = false,
        val jsStatic: Boolean = false,
        propagateAnnotations: Boolean = true,
        annotationsToPropagate: Set<FunctionAnnotation> = emptySet(),
        optIns: Set<AnnotationSpec> = emptySet(),
        customFileHeader: String = KTORG_GENERATED_FILE_COMMENT,
        customClassHeader: String = KTORG_GENERATED_COMMENT,
    ) : GenOptions(propagateAnnotations, annotationsToPropagate, optIns, customClassHeader)
}
