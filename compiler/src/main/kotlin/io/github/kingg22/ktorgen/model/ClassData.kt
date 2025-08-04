package io.github.kingg22.ktorgen.model

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.squareup.kotlinpoet.AnnotationSpec
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
    val annotationSet: Set<KSAnnotation>,
    ksClassDeclaration: KSClassDeclaration,
    val superClasses: List<KSTypeReference> = emptyList(),
    val properties: List<KSPropertyDeclaration> = emptyList(),
    val modifierSet: Set<KModifier> = emptySet(),
    val haveCompanionObject: Boolean = false,
    goingToGenerate: Boolean = true,
    generatedName: String = "_${interfaceName}Impl",
    visibilityModifier: String = "public",
    generateTopLevelFunction: Boolean = true,
    generateCompanionFunction: Boolean = false,
    generateExtensions: Boolean = false,
    jvmStatic: Boolean = false,
    jsStatic: Boolean = false,
    generatePublicConstructor: Boolean = false,
    propagateAnnotations: Boolean = true,
    annotationsToPropagate: Set<AnnotationSpec> = emptySet(),
    optIns: Set<AnnotationSpec> = emptySet(),
    customFileHeader: String = KTORG_GENERATED_FILE_COMMENT,
    customClassHeader: String = "",
) : GenOptions.GenTypeOption(
    goingToGenerate = goingToGenerate,
    generatedName = generatedName,
    visibilityModifier = visibilityModifier,
    generateTopLevelFunction = generateTopLevelFunction,
    generateCompanionFunction = generateCompanionFunction,
    generateExtensions = generateExtensions,
    jvmStatic = jvmStatic,
    jsStatic = jsStatic,
    generatePublicConstructor = generatePublicConstructor,
    propagateAnnotations = propagateAnnotations,
    annotationsToPropagate = annotationsToPropagate,
    optIns = optIns,
    customFileHeader = customFileHeader,
    customClassHeader = customClassHeader,
) {
    val haveHttpClientProperty by lazy { properties.any { it.type.resolve().toClassName() == HttpClientClassName } }
}
