package io.github.kingg22.ktorgen.validator

import io.github.kingg22.ktorgen.model.FunctionData

class ValidationContext(
    val className: String,
    val packageName: String,
    val functions: List<FunctionData>,
    val superInterfaces: List<String>,
    val baseUrl: String?,
)
