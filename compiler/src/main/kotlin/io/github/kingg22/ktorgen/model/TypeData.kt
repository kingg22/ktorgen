@file:OptIn(ExperimentalContracts::class)

package io.github.kingg22.ktorgen.model

import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class TypeData(val parameterType: KSType, val typeName: TypeName = parameterType.toTypeName())

private val FLOW_CLASS = ClassName("kotlinx.coroutines.flow", "Flow")
private val RESULT_CLASS = ClassName("kotlin", "Result")

fun TypeName.isFlowType(): Boolean {
    contract { returns(true) implies (this@isFlowType is ParameterizedTypeName) }
    return this is ParameterizedTypeName && (rawType == FLOW_CLASS)
}

fun TypeName.isResultType(): Boolean {
    contract { returns(true) implies (this@isResultType is ParameterizedTypeName) }
    return this is ParameterizedTypeName && (rawType == RESULT_CLASS)
}

fun TypeName.unwrapFlow(): TypeName {
    require(isFlowType()) { "unwrapFlow() llamado en un tipo que no es Flow" }
    return typeArguments.first()
}

fun TypeName.unwrapResult(): TypeName {
    require(isResultType()) { "unwrapResult() llamado en un tipo que no es Result" }
    return typeArguments.first()
}

/** Caso especial para Flow<Result<T>> → devuelve T */
fun TypeName.unwrapFlowResult(): TypeName {
    require(isFlowType()) { "unwrapFlowResult() requiere un Flow" }
    val inner = unwrapFlow()
    require(inner.isResultType()) { "unwrapFlowResult() requiere un Flow<Result<...>>" }
    return inner.typeArguments.first()
}
