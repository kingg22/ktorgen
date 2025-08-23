package io.github.kingg22.ktorgen.validator

import io.github.kingg22.ktorgen.model.ClassData
import io.github.kingg22.ktorgen.model.FunctionData

class ValidationContext(val classData: ClassData) {
    // shortcuts
    val functions: List<FunctionData> = classData.functions
}
