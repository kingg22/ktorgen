package io.github.kingg22.ktorgen.extractor

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ksp.toKModifier
import com.squareup.kotlinpoet.ksp.toTypeName
import io.github.kingg22.ktorgen.KtorGenLogger
import io.github.kingg22.ktorgen.model.ClassData
import io.github.kingg22.ktorgen.model.KTOR_CLIENT_CALL_BODY
import io.github.kingg22.ktorgen.model.KTOR_CLIENT_REQUEST
import io.github.kingg22.ktorgen.model.KTOR_DECODE_URL_QUERY
import io.github.kingg22.ktorgen.model.KTOR_URL_TAKE_FROM

class ClassMapper : DeclarationMapper {
    override fun mapToModel(declaration: KSClassDeclaration): ClassData {
        val imports = mutableSetOf<String>()

        val packageName = declaration.packageName.asString()
        val className = declaration.simpleName.asString()

        val functions = declaration.getDeclaredFunctions().map { func ->
            DeclarationFunctionMapper.DEFAULT.mapToModel(func, imports::add)
        }

        val filteredSupertypes = declaration.superTypes.filterNot {
            /** In KSP Any is a supertype of an interface */
            it.toTypeName() == ANY
        }

        val properties = declaration.getDeclaredProperties()

        // an operation terminal of sequences must be in one site
        return ClassData(
            ksClassDeclaration = declaration,
            interfaceName = className,
            packageNameString = packageName,
            functions = functions.toList().also {
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
            },
            imports = imports,
            superClasses = filteredSupertypes.toList(),
            properties = properties.toList(),
            modifierSet = declaration.modifiers.mapNotNull { it.toKModifier() }.toSet(),
            ksFile = requireNotNull(declaration.containingFile) { KtorGenLogger.INTERFACE_NOT_HAVE_FILE + className },
            annotationSet = declaration.annotations.toSet(),
            haveCompanionObject = declaration.declarations
                .filterIsInstance<KSClassDeclaration>()
                .any { it.isCompanionObject },
        )
    }
}
