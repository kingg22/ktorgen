package io.github.kingg22.ktorgen.model

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ksp.toTypeName
import io.github.kingg22.ktorgen.model.options.AnnotationsOptions
import io.github.kingg22.ktorgen.model.options.ClassGenerationOptions
import io.github.kingg22.ktorgen.model.options.Factories
import io.github.kingg22.ktorgen.model.options.VisibilityOptions

/**
 * Contain all information of source code to generate a class implementing the interface
 * @param interfaceName name of the interface that contains annotations
 * @param superClasses qualifiedNames of interface
 * @param qualifiedName fully qualified name of the interface
 */
class ClassData(
    val packageNameString: String,
    val interfaceName: String,
    val functions: Sequence<FunctionData>,
    val ksFile: KSFile,
    val ksInterface: KSClassDeclaration,
    val superClasses: Sequence<KSTypeReference>,
    val properties: Sequence<KSPropertyDeclaration>,
    val modifierSet: Set<KModifier>,
    val ksCompanionObject: KSClassDeclaration?,
    val factories: Set<Factories>,
    val qualifiedName: String,
    val options: ClassGenerationOptions,
    val annotationsOptions: AnnotationsOptions,
    val visibilityOptions: VisibilityOptions,
) {
    val generateTopLevelFunction: Boolean get() = factories.any { it is Factories.TopLevelFactory }
    val generateCompanionExtFunction: Boolean get() = factories.any { it is Factories.CompanionExtension }
    val generateHttpClientExtension: Boolean get() = factories.any { it is Factories.HttpClientExtension }
    val expectFunctions: Sequence<KSFunctionDeclaration> get() = factories.filterIsInstance<Factories.KmpExpectActual>()
        .flatMap { it.expectFunctions }.asSequence()

    val httpClientProperty by lazy(LazyThreadSafetyMode.NONE) {
        properties.firstOrNull {
            val type = it.type.resolve()
            if (type.isError) return@firstOrNull false
            type.toTypeName() == HttpClientClassName
        }
    }
}
