// import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    // alias(libs.plugins.kotlinxKover)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.mavenPublish)
}

group = "io.github.kingg22"
version = libs.versions.ktorgen.version.get()

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        extraWarnings.set(true)
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    implementation(projects.annotations)
    implementation(libs.ksp.api)
    implementation(libs.kotlin.poet)
    implementation(libs.kotlin.poet.ksp)

    // temporal remove until add unit test
    // testImplementation(kotlin("test"))
    // testImplementation(libs.androidx.room.compiler.testing)
}

ktlint {
    version.set(libs.versions.ktlint.pinterest.get())
}
/*
kover {
    reports.total {
        verify {
            // temporal disable
            rule("Basic Line Coverage") {
                disabled.set(true)
                minBound(60, CoverageUnit.LINE)
            }

            rule("Basic Branch Coverage") {
                disabled.set(true)
                minBound(20, CoverageUnit.BRANCH)
            }
        }
    }
}
 */

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "ktorgen-compiler", version.toString())

    pom {
        name = "KtorGen – KSP Annotation Processor"
        description =
            "A Kotlin Symbol Processing (KSP) that generates Ktor Client implementations from annotated interfaces."
        inceptionYear = "2025"
        url = "https://github.com/kingg22/ktorgen"
        licenses {
            license {
                name = "The Apache Software License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id = "kingg22"
                name = "Rey Acosta (Kingg22)"
                url = "https://github.com/kingg22"
            }
        }
        scm {
            url = "https://github.com/kingg22/ktorgen"
            connection = "scm:git:git://github.com/kingg22/ktorgen.git"
            developerConnection = "scm:git:ssh://git@github.com/kingg22/ktorgen.git"
        }
    }
}
