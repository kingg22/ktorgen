package io.github.kingg22.ktorgen.validator

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.squareup.kotlinpoet.KModifier
import io.github.kingg22.ktorgen.model.ClassData
import io.github.kingg22.ktorgen.model.ClassGenerationOptions

class ValidationContext(classData: ClassData) {
    // shortcuts
    val classData = SnapshotClassData(classData)
    val expectFunctions = classData.expectFunctions
    val functions = classData.functions

    class SnapshotClassData private constructor(
        val packageNameString: String,
        val interfaceName: String,
        val modifierSet: Set<KModifier>,
        val companionObjectDeclaration: KSClassDeclaration?,
        val isKtorGenOnClass: Boolean,
        val isKtorGenOnCompanionObject: Boolean,
        options: ClassGenerationOptions,
        ksClass: KSNode,
    ) : ClassGenerationOptions(options),
        KSNode by ksClass {
        constructor(
            classData: ClassData,
        ) : this(
            packageNameString = classData.packageNameString,
            interfaceName = classData.interfaceName,
            modifierSet = classData.modifierSet,
            companionObjectDeclaration = classData.companionObjectDeclaration,
            isKtorGenOnClass = classData.isKtorGenAnnotationDeclaredOnClass,
            isKtorGenOnCompanionObject = classData.isKtorGenAnnotationDeclaredOnCompanionClass,
            options = classData,
            ksClass = classData.ksClassDeclaration,
        )
    }
}
