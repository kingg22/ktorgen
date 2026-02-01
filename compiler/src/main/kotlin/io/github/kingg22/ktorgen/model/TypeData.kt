@file:JvmName("TypeDataExt")

package io.github.kingg22.ktorgen.model

import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toTypeName
import io.github.kingg22.ktorgen.checkImplementation
import kotlin.contracts.contract

class TypeData(val parameterType: KSType) {
    val typeName = parameterType.toTypeName()
}

private val FLOW_CLASS = ClassName("kotlinx.coroutines.flow", "Flow")
val RESULT_CLASS = ClassName("kotlin", "Result")

fun TypeName.isFlowType(): Boolean {
    contract { returns(true) implies (this@isFlowType is ParameterizedTypeName) }
    return this is ParameterizedTypeName && (rawType == FLOW_CLASS)
}

fun TypeName.isResultType(): Boolean {
    contract { returns(true) implies (this@isResultType is ParameterizedTypeName) }
    return this is ParameterizedTypeName && (rawType == RESULT_CLASS)
}

fun ParameterizedTypeName.unwrapFlow(): TypeName {
    checkImplementation(isFlowType()) { "unwrapFlow() llamado en un tipo que no es Flow" }
    return typeArguments.first()
}

fun ParameterizedTypeName.unwrapResult(): TypeName {
    checkImplementation(isResultType()) { "unwrapResult() llamado en un tipo que no es Result" }
    return typeArguments.first()
}

/** Caso especial para `Flow<Result<T>>` → devuelve T */
fun ParameterizedTypeName.unwrapFlowResult(): TypeName {
    checkImplementation(isFlowType()) { "unwrapFlowResult() requiere un Flow" }
    val inner = unwrapFlow()
    checkImplementation(inner.isResultType()) { "unwrapFlowResult() requiere un Flow<Result<...>>" }
    return inner.typeArguments.first()
}

val TypeName.isHttpRequestBuilderType get() = this == HttpRequestBuilderTypeName

val TypeName.isHttpStatementType get() = this == HttpStatementClassName
