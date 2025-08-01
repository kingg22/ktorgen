package io.github.kingg22.ktorgen.model

import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toTypeName

class ReturnType(val parameterType: KSType, val typeName: TypeName = parameterType.toTypeName())
