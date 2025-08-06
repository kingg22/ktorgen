package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ksp.toKModifier
import com.squareup.kotlinpoet.ksp.toTypeName
import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.Timer
import io.github.kingg22.ktorgen.model.ClassData
import io.github.kingg22.ktorgen.model.KTOR_CLIENT_CALL_BODY
import io.github.kingg22.ktorgen.model.KTOR_CLIENT_REQUEST
import io.github.kingg22.ktorgen.model.KTOR_DECODE_URL_QUERY
import io.github.kingg22.ktorgen.model.KTOR_URL_TAKE_FROM

class ClassMapper : DeclarationMapper {
    override fun mapToModel(declaration: KSClassDeclaration): ClassData {
        val timer = Timer("KtorGen [Class Mapper] for ${declaration.simpleName.asString()}").start()
        try {
            val imports = mutableSetOf<String>()

            val packageName = declaration.packageName.asString()
            val className = declaration.simpleName.asString()
            timer.markStepCompleted("Processing $className")

            val functions = declaration.getDeclaredFunctions().map { func ->
                DeclarationFunctionMapper.DEFAULT.mapToModel(func, imports::add).also {
                    timer.markStepCompleted("Processed function: ${it.name}")
                }
            }.toList().also {
                if (it.isNotEmpty()) {
                    imports.addAll(
                        arrayOf(
                            KTOR_CLIENT_CALL_BODY,
                            KTOR_CLIENT_REQUEST,
                            KTOR_URL_TAKE_FROM,
                            KTOR_DECODE_URL_QUERY,
                        ),
                    )
                }
            }
            timer.markStepCompleted("Processed all functions")

            val filteredSupertypes = declaration.superTypes.filterNot { it.toTypeName() == ANY }
            timer.markStepCompleted("Retrieved all supertypes")

            val companionObject = declaration.declarations
                .filterIsInstance<KSClassDeclaration>()
                .any { it.isCompanionObject }
            timer.markStepCompleted("Have companion object for ${declaration.simpleName.asString()}: $companionObject")

            val properties = declaration.getDeclaredProperties()

            timer.markStepCompleted("Retrieved all properties, building class data")
            // an operation terminal of sequences must be in one site
            return ClassData(
                ksClassDeclaration = declaration,
                interfaceName = className,
                packageNameString = packageName,
                functions = functions,
                imports = imports,
                superClasses = filteredSupertypes.toList(),
                properties = properties.toList(),
                modifierSet = declaration.modifiers.mapNotNull { it.toKModifier() }.toSet(),
                ksFile = requireNotNull(declaration.containingFile) {
                    KtorGenLogger.INTERFACE_NOT_HAVE_FILE + className
                },
                annotationSet = declaration.annotations.toSet(),
                visibilityModifier = declaration.getVisibility(),
                haveCompanionObject = companionObject,
            ).also {
                timer.markStepCompleted("Mapper complete of ${it.interfaceName} to ${it.generatedName}")
                timer.finishAndPrint()
            }
        } catch (e: Exception) {
            timer.markStepCompleted("Error on interface ${declaration.simpleName.asString()}")
            throw e
        } finally {
            timer.finishAndPrint()
        }
    }
}
