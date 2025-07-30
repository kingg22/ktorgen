package io.github.kingg22.ktorgen.model

import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.TypeName

class ReturnType(val name: String, val parameterType: KSType, val typeName: TypeName? = null)
