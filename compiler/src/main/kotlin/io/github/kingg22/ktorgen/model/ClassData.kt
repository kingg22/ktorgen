package io.github.kingg22.ktorgen.model

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ksp.toClassName

/**
 * Contain all information of source code to generate a class implementing the interface
 * @param interfaceName name of the interface that contains annotations
 * @param superClasses qualifiedNames of interface
 */
class ClassData(
    val packageNameString: String,
    val interfaceName: String,
    val functions: Sequence<FunctionData>,
    val ksFile: KSFile,
    val ksClassDeclaration: KSClassDeclaration,
    val superClasses: Sequence<KSTypeReference>,
    val properties: Sequence<KSPropertyDeclaration>,
    val modifierSet: Set<KModifier>,
    val companionObjectDeclaration: KSClassDeclaration?,
    val expectFunctions: Sequence<KSFunctionDeclaration>,
    val isKtorGenAnnotationDeclaredOnClass: Boolean,
    val isKtorGenAnnotationDeclaredOnCompanionClass: Boolean,
    val qualifiedName: String,
    options: ClassGenerationOptions,
) : ClassGenerationOptions(options) {
    val httpClientProperty by lazy {
        properties.firstOrNull { it.type.resolve().toClassName() == HttpClientClassName }
    }
}
