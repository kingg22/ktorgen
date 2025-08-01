package io.github.kingg22.ktorgen.validator

import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.KtorGenOptions
import io.github.kingg22.ktorgen.model.ClassData

fun interface Validator {
    fun validate(classData: ClassData, ktorGenOptions: KtorGenOptions, ktorGenLogger: KtorGenLogger): ClassData?

    companion object {
        val DEFAULT by lazy {
            Validator { data, _, _ -> data }
        }

        fun validate(classData: ClassData, ktorGenOptions: KtorGenOptions, ktorGenLogger: KtorGenLogger) =
            DEFAULT.validate(classData, ktorGenOptions, ktorGenLogger)
    }
}
