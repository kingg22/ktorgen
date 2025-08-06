package io.github.kingg22.ktorgen.validator

import io.github.kingg22.ktorgen.model.FunctionData

class ValidationContext(
    val className: String,
    val packageName: String,
    val functions: List<FunctionData>,
    val visibility: String,
    val baseUrl: String?,
)
