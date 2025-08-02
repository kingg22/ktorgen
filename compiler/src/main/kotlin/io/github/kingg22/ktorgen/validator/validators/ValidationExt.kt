@file:JvmName("ValidationExt")
@file:JvmMultifileClass

package io.github.kingg22.ktorgen.validator.validators

import io.github.kingg22.ktorgen.model.FunctionData
import io.github.kingg22.ktorgen.model.ParameterData
import io.github.kingg22.ktorgen.validator.ValidationContext

fun addDeclaration(context: ValidationContext, function: FunctionData, parameter: ParameterData? = null) = buildString {
    appendLine()
    append("Declaration: ")
    append(context.className)
    append(".")
    append(function.ktorGenAnnotations + function.httpMethodAnnotation)
    append(function.name)
    append("(")
    function.parameterDataList.forEachIndexed { index, parameterData ->
        append(parameterData.annotations)
        append(" ")
        if (parameter != null && parameter == parameterData) {
            append("**")
            append(parameterData.name)
            append("**")
        } else {
            append(parameterData.name)
        }
        append(": ")
        append(parameterData.type.parameterType.declaration.simpleName.getShortName())
        if (parameterData.type.parameterType.isMarkedNullable) append("?")
        if (index != function.parameterDataList.indices.last) append(", ")
    }
    append(")")
    append(": ")
    val returnType = function.returnType.parameterType
    append(returnType.declaration.simpleName.getShortName())
    if (returnType.isMarkedNullable) append("?")
}

/**
 * Validate the generic types of the parameter with <*, *>
 * @return Pair (first, second) where is (type, nullability)
 */
fun validateArgsOf(parameter: ParameterData): Pair<Pair<String?, Boolean>, Pair<String?, Boolean>> {
    val args = parameter.type.parameterType.arguments
    val firstType = args.getOrNull(0)?.type?.resolve()?.let {
        Pair(it.declaration.qualifiedName?.asString(), it.isMarkedNullable)
    } ?: Pair(null, false)
    val secondType = args.getOrNull(1)?.type?.resolve()?.let {
        Pair(it.declaration.qualifiedName?.asString(), it.isMarkedNullable)
    } ?: Pair(null, false)
    return firstType to secondType
}
