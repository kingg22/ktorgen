package io.github.kingg22.ktorgen.model

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
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
    val functions: List<FunctionData>,
    val imports: Set<String>,
    val ksFile: KSFile,
    val ksClassDeclaration: KSClassDeclaration,
    val superClasses: List<KSTypeReference>,
    val properties: List<KSPropertyDeclaration>,
    val modifierSet: Set<KModifier>,
    val haveCompanionObject: Boolean,
    val options: GenOptions.GenTypeOption,
) : GenOptions.GenTypeOption by options {
    val httpClientProperty by lazy {
        properties.firstOrNull { it.type.resolve().toClassName() == HttpClientClassName }
    }

    // TODO change parent to use this
    class ClassGenerationOptions(
        val generatedName: String,
        val generate: Boolean = true,
        val visibility: String = "public",
        val annotations: Set<AnnotationSpec> = emptySet(),
        val optIns: Set<ClassName> = emptySet(),
        val optInAnnotation: AnnotationSpec? = null,
        val customHeader: String = "",
        val basePath: String = "",
        val generateTopLevelFunction: Boolean = true,
        val generateCompanionExtFunction: Boolean = false,
        val generateHttpClientExtension: Boolean = false,
    )
}
