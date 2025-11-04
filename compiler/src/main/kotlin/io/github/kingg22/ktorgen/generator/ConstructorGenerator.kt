package io.github.kingg22.ktorgen.generator

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import io.github.kingg22.ktorgen.model.ClassData
import io.github.kingg22.ktorgen.model.HttpClientClassName

class ConstructorGenerator {
    /** @return FunSpec del constructor, propiedades y nombre de la propiedad httpClient */
    fun generatePrimaryConstructorAndProperties(
        classData: ClassData,
        visibilityModifier: KModifier,
    ): ConstructorAndProperties {
        val primaryConstructorBuilder = FunSpec.constructorBuilder().addModifiers(visibilityModifier)
        val propertiesToAdd = mutableListOf<PropertySpec>()
        var httpClientName = "_httpClient"

        // HttpClient property
        classData.httpClientProperty?.let {
            httpClientName = it.simpleName.asString()
            primaryConstructorBuilder.addParameter(httpClientName, HttpClientClassName)
            propertiesToAdd += PropertySpec.builder(httpClientName, HttpClientClassName)
                .addModifiers(OVERRIDE)
                .initializer("%L", httpClientName)
                .build()
        } ?: run {
            primaryConstructorBuilder.addParameter(httpClientName, HttpClientClassName)
            propertiesToAdd += PropertySpec.builder(httpClientName, HttpClientClassName)
                .addModifiers(PRIVATE)
                .initializer("%L", httpClientName)
                .build()
        }

        // Other properties
        classData.properties
            .filter { it.type.toTypeName() != HttpClientClassName }
            .forEach { property ->
                val paramName = property.simpleName.asString()
                val typeName = property.type.toTypeName()

                primaryConstructorBuilder.addParameter(paramName, typeName)
                propertiesToAdd += PropertySpec.builder(paramName, typeName)
                    .addModifiers(OVERRIDE)
                    .initializer("%L", paramName)
                    .mutable(property.isMutable)
                    .build()
            }

        return ConstructorAndProperties(
            primaryConstructorBuilder,
            propertiesToAdd,
            MemberName(classData.packageNameString, httpClientName),
        )
    }

    /** Computes constructor signature for validation */
    fun computeConstructorSignature(classData: ClassData): List<TypeName> = buildList {
        add(HttpClientClassName)
        classData.properties
            .map { it.type.toTypeName() }
            .filter { it != HttpClientClassName }
            .forEach { add(it) }
        classData.superClasses.forEach { ref ->
            add(ref.resolve().toClassName())
        }
    }

    data class ConstructorAndProperties(
        val constructor: FunSpec.Builder,
        val properties: List<PropertySpec>,
        val httpClientName: MemberName,
    )
}
