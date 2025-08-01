package io.github.kingg22.ktorgen.generator

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import io.github.kingg22.ktorgen.model.ClassData

fun interface KtorGenGenerator {
    fun generate(classData: ClassData): FileSpec

    companion object {
        val DEFAULT by lazy { rawKtorGenGenerator }

        val rawKtorGenGenerator = KtorGenGenerator { classData ->
            val interfaceName = classData.interfaceName

            val classBuilder = TypeSpec.classBuilder(classData.generatedName)
                .addModifiers(KModifier.PUBLIC)
                .addSuperinterface(ClassName(classData.packageName, interfaceName))
                .addKdoc(classData.customHeader)

            classData.functions.forEach { func ->
                val funBuilder = FunSpec.builder(func.name)
                    .addModifiers(func.modifiers)
                    .returns(func.returnType.typeName)
                    .addKdoc(func.customHeader)

                if (func.isSuspend) funBuilder.addModifiers(KModifier.SUSPEND)

                func.parameterDataList.forEach { param ->
                    funBuilder.addParameter(param.name, param.type.typeName)
                }

                funBuilder.addCode("return TODO()")

                classBuilder.addFunction(funBuilder.build())
            }

            val fileBuilder = FileSpec.builder(classData.packageName, classData.generatedName)
                .addFileComment(classData.customHeader)
                .addAnnotation(
                    AnnotationSpec.builder(Suppress::class)
                        .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                        .addMember("%S", "REDUNDANT_VISIBILITY_MODIFIER")
                        .addMember("%S", "unused")
                        .addMember("%S", "UNUSED_IMPORT")
                        .build(),
                )

            classData.imports.forEach { fileBuilder.addImport(it.substringBeforeLast("."), it.substringAfterLast(".")) }

            fileBuilder.addType(classBuilder.build()).build()
        }

        val NO_OP by lazy {
            KtorGenGenerator { data ->
                FileSpec.builder(data.packageName, data.generatedName)
                    .addFileComment(
                        "This class is generated to test the KtorGen compiler. It does not contain any code and should not be used.",
                    )
                    .addProperty("hello", String::class, KModifier.PRIVATE, KModifier.CONST)
                    .build()
            }
        }
    }
}
