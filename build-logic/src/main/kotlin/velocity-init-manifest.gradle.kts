import java.io.ByteArrayOutputStream

abstract class VelocityInitManifestGradle @Inject constructor(
    private val execOps: ExecOperations, project: Project) {
    init {
        val currentShortRevision = ByteArrayOutputStream().use { output ->
            execOps.exec {
                commandLine("git", "rev-parse", "HEAD")
                standardOutput = output
            }
            output.toString().trim().take(8)
        }

        project.tasks.withType<Jar>().configureEach {
            manifest {
                val buildNumber = System.getenv("BUILD_NUMBER")
                val velocityHumanVersion = if (project.version.toString().endsWith("-SNAPSHOT")) {
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
    }
}
