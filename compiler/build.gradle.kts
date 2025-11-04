import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.sonarqube.gradle.SonarTask

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinxKover)
    alias(libs.plugins.mavenPublish)
}

group = "io.github.kingg22"
description = "A Kotlin Symbol Processing (KSP) that generates Ktor Client implementations from annotated interfaces."
version = libs.versions.ktorgen.version.get()

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_2)
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
        jvmDefault.set(JvmDefaultMode.NO_COMPATIBILITY)
        jvmTarget.set(JvmTarget.JVM_11)
        extraWarnings.set(true)
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    implementation(projects.annotations)
    implementation(kotlin("reflect"))
    implementation(libs.ksp.api)
    implementation(libs.kotlin.poet)
    implementation(libs.kotlin.poet.ksp)

    testImplementation(kotlin("test"))
    testImplementation(libs.androidx.room.compiler.testing)
    testImplementation(libs.google.truth)
    testRuntimeClasspath(libs.ktor.client.core)
}

kover {
    reports.total {
        verify {
            rule("Basic Line Coverage") {
                minBound(60, CoverageUnit.LINE)
            }

            rule("Basic Branch Coverage") {
                minBound(20, CoverageUnit.BRANCH)
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.compileTestKotlin {
    compilerOptions.optIn.add("androidx.room.compiler.processing.ExperimentalProcessingApi")
}

rootProject.tasks.named<SonarTask>("sonar") {
    dependsOn(tasks.koverXmlReport)
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "ktorgen-compiler", version.toString())

    pom {
        name.set("KtorGen – KSP Annotation Processor")
        description.set(project.description)
        inceptionYear.set("2025")
        url.set("https://github.com/kingg22/ktorgen")
        licenses {
            license {
                name.set("The Apache Software License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("kingg22")
                name.set("Rey Acosta (Kingg22)")
                url.set("https://github.com/kingg22")
            }
        }
        scm {
            url.set("https://github.com/kingg22/ktorgen")
            connection.set("scm:git:git://github.com/kingg22/ktorgen.git")
            developerConnection.set("scm:git:ssh://git@github.com/kingg22/ktorgen.git")
        }
    }
}
