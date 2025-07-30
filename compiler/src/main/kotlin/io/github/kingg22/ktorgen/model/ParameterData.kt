package io.github.kingg22.ktorgen.model

import io.github.kingg22.ktorgen.model.annotations.ParameterAnnotation

class ParameterData(val name: String, val type: ReturnType, val annotations: List<ParameterAnnotation> = emptyList()) {
    inline fun <reified T : ParameterAnnotation> findAnnotationOrNull(): T? =
        this.annotations.filterIsInstance<T>().firstOrNull()

    inline fun <reified T : ParameterAnnotation> hasAnnotation(): Boolean = this.findAnnotationOrNull<T>() != null
}
