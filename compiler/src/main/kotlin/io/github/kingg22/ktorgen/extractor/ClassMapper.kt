package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ksp.toKModifier
import com.squareup.kotlinpoet.ksp.toTypeName
import io.github.kingg22.ktorgen.DiagnosticTimer
import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.model.ClassData
import io.github.kingg22.ktorgen.model.KTOR_CLIENT_CALL_BODY
import io.github.kingg22.ktorgen.model.KTOR_CLIENT_REQUEST
import io.github.kingg22.ktorgen.model.KTOR_DECODE_URL_QUERY
import io.github.kingg22.ktorgen.model.KTOR_URL_TAKE_FROM

class ClassMapper : DeclarationMapper {
    override fun mapToModel(
        declaration: KSClassDeclaration,
        timer: (String) -> DiagnosticTimer.DiagnosticSender,
    ): ClassData {
        val interfaceName = declaration.simpleName.asString()
        val timer = timer("Class Mapper for [$interfaceName]")
        return timer.work { _ ->
            val imports = mutableSetOf<String>()

            val packageName = declaration.packageName.asString()

            val functions = declaration.getDeclaredFunctions().map { func ->
                DeclarationFunctionMapper.DEFAULT.mapToModel(func, imports::add) { timer.createTask(it) }.also {
                    timer.addStep("Processed function: ${it.name}")
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
            timer.addStep("Processed all functions")

            val filteredSupertypes = declaration.superTypes.filterNot { it.toTypeName() == ANY }
            timer.addStep("Retrieved all supertypes")

            val companionObject = declaration.declarations
                .filterIsInstance<KSClassDeclaration>()
                .any { it.isCompanionObject }
            timer.addStep("Have companion object for: $companionObject")

            val properties = declaration.getDeclaredProperties()

            timer.addStep("Retrieved all properties")
            // an operation terminal of sequences must be in one site
            ClassData(
                ksClassDeclaration = declaration,
                interfaceName = interfaceName,
                packageNameString = packageName,
                functions = functions,
                imports = imports,
                superClasses = filteredSupertypes.toList(),
                properties = properties.toList(),
                modifierSet = declaration.modifiers.mapNotNull { it.toKModifier() }.toSet(),
                ksFile = timer.requireNotNull(
                    declaration.containingFile,
                    KtorGenLogger.INTERFACE_NOT_HAVE_FILE + interfaceName,
                ),
                annotationSet = declaration.annotations.toSet(),
                visibilityModifier = declaration.getVisibility(),
                haveCompanionObject = companionObject,
            ).also {
                timer.addStep("Mapper complete of ${it.interfaceName} to ${it.generatedName}")
            }
        }
    }
}
