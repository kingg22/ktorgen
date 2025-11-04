plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinxKover) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.mavenPublish) apply false
    alias(libs.plugins.sonarqube)
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "org.sonarqube")

    dependencyLocking {
        lockAllConfigurations()
    }

    sonar.run {
        // ignora completamente estos módulos
        if (project.name == "example" || project.name == "sample") {
            isSkipProject = true
        }

        properties {
            // evita incluir coverage de módulos que no tienen tests
            if (project.name == "annotations") {
                property("sonar.sources", "src/commonMain")
                property("sonar.coverage.exclusions", "**/*")
            }

            if (project.name == "compiler") {
                property("sonar.sources", "src/main")
                property("sonar.tests", "src/test")
                property(
                    "sonar.coverage.jacoco.xmlReportPaths",
                    project.layout.buildDirectory.file("reports/kover/report.xml").get().asFile.absolutePath,
                )
            }
        }
    }
}

sonar.properties {
    property("sonar.projectKey", "kingg22_ktorgen")
    property("sonar.organization", "kingg22")
    property("sonar.projectName", "ktorgen")
    property("sonar.projectVersion", libs.versions.ktorgen.version.get())
    property("sonar.gradle.scanAll", false) // Conflict with submodules config
    property("sonar.scanner.skipJreProvisioning", true)
    property("sonar.scanner.javaExePath", "${System.getProperty("java.home")}/bin/java")
}

tasks.sonar {
    onlyIf {
        System.getProperty("java.home") != null && System.getenv("SONAR_TOKEN") != null
    }
}

ktlint {
    version.set(libs.versions.ktlint.pinterest.get())
}

dependencyLocking {
    lockAllConfigurations()
}
