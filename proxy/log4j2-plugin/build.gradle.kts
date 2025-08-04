dependencies {
    implementation(libs.bundles.log4j)
    annotationProcessor(libs.log4j.core)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(
        listOf(
            "-Alog4j.graalvm.groupId=org.apache.logging.log4j",
            "-Alog4j.graalvm.artifactId=log4j-core"
        )
    )
}
