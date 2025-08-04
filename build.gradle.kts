plugins {
    `java-library`
    id("velocity-checkstyle") apply false
    id("velocity-spotless") apply false
}

subprojects {
    apply<JavaLibraryPlugin>()

    apply(plugin = "velocity-checkstyle")
    apply(plugin = "velocity-spotless")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    dependencies {
        testImplementation(rootProject.libs.junit.jupiter.api)
        testRuntimeOnly(rootProject.libs.junit.jupiter.engine)
        testImplementation(rootProject.libs.junit.platform.launcher)
        testImplementation(rootProject.libs.junit.platform.engine)
    }

    testing.suites.named<JvmTestSuite>("test") {
        useJUnitJupiter()
        targets.all {
            testTask.configure {
                reports.junitXml.required = true
            }
        }
    }
}
