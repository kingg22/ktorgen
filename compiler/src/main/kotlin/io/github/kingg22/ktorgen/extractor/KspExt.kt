package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.symbol.KSAnnotation

/** Safe get and cast the properties of annotation */
inline fun <reified T> KSAnnotation.getArgumentValueByName(name: String): T? = this.arguments.firstOrNull {
    it.name?.asString() == name && it.value != null && it.value is T
}?.value as? T
