@file:JvmName("ValidationExt")
@file:JvmMultifileClass

package io.github.kingg22.ktorgen.validator.validators

import io.github.kingg22.ktorgen.model.ParameterData
import io.github.kingg22.ktorgen.validator.ValidationResult

const val KOTLIN_STRING = "kotlin.String"

/** Validate a [Map] or [Pair] types */
fun ValidationResult.validateMapParameter(
    parameter: ParameterData,
    errorMessage: String,
    /** Condition to raise error */
    validation: (Pair<String?, Boolean>, Pair<String?, Boolean>) -> Boolean = { keys, _ ->
        keys != Pair(KOTLIN_STRING, false)
    },
) {
    val decl = parameter.typeData.parameterType.declaration
    val qualifiedName = decl.qualifiedName?.asString()
    if (qualifiedName == "kotlin.collections.Map" || qualifiedName == "kotlin.Pair") {
        val (firstType, secondType) = validateArgsOf(parameter)
        if (validation(firstType, secondType)) {
            addError(errorMessage, parameter.ksValueParameter)
        }
    } else {
        addError(errorMessage, parameter.ksValueParameter)
    }
}

/**
 * Validate the generic types of the parameter with <*, *>
 * @return Pair (first, second) where is (type, nullability)
 */
private fun validateArgsOf(parameter: ParameterData): Pair<Pair<String?, Boolean>, Pair<String?, Boolean>> {
    val args = parameter.typeData.parameterType.arguments
    val firstType = args.getOrNull(0)?.type?.resolve()?.let {
        Pair(it.declaration.qualifiedName?.asString(), it.isMarkedNullable)
    } ?: Pair(null, false)
    val secondType = args.getOrNull(1)?.type?.resolve()?.let {
        Pair(it.declaration.qualifiedName?.asString(), it.isMarkedNullable)
    } ?: Pair(null, false)
    return firstType to secondType
}
