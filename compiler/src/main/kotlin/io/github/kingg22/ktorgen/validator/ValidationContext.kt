package io.github.kingg22.ktorgen.validator

import io.github.kingg22.ktorgen.model.ClassData
import io.github.kingg22.ktorgen.model.FunctionData

class ValidationContext(val classData: ClassData) {
    // shortcuts
    val className: String = classData.generatedName
    val functions: List<FunctionData> = classData.functions
    val visibilityGeneratedClass: String = classData.visibilityModifier
}
