import java.io.ByteArrayOutputStream

val currentShortRevision = ByteArrayOutputStream().use {
    @Suppress("DEPRECATION")
    exec {
        executable = "git"
        args = listOf("rev-parse", "HEAD")
        standardOutput = it
    }
    it.toString().trim().substring(0, 8)
}

tasks.withType<Jar> {
    manifest {
        val buildNumber = System.getenv("BUILD_NUMBER")
        val velocityHumanVersion: String =
            if (project.version.toString().endsWith("-SNAPSHOT")) {
                if (buildNumber == null) {
                    "${project.version} (git-$currentShortRevision)"
                } else {
                    "${project.version} (git-$currentShortRevision-b$buildNumber)"
                }
            } else {
                archiveVersion.get()
            }
        attributes["Implementation-Version"] = velocityHumanVersion
    }
}
