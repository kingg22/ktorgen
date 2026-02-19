package io.github.kingg22.ktorgen.validator

import io.github.kingg22.ktorgen.model.ClassData

class ValidationContext(val classData: ClassData) {
    // shortcuts
    val expectFunctions inline get() = classData.expectFunctions
    val functions inline get() = classData.functions
}
