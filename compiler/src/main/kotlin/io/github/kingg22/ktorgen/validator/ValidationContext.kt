package io.github.kingg22.ktorgen.validator

import io.github.kingg22.ktorgen.model.ClassData
import io.github.kingg22.ktorgen.model.FunctionData

class ValidationContext(internal val classData: ClassData) {
    // shortcuts
    internal val functions: List<FunctionData> = classData.functions
}
