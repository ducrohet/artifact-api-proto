package third.party.plugin

import core.api.*
import org.gradle.api.Plugin
import org.gradle.api.Project

class CustomPlugin: Plugin<Project> {

    override fun apply(project: Project) {
        project.plugins.withType(PublicPlugin::class.java) {
            val extension = project.extensions.getByType(Extension::class.java)

            extension.handleArtifacts { holder ->
                if (project.properties["android.transform"] == "true") {

                    // ------
                    // Directory Transform
                    holder.transform(
                            SingleDirectoryArtifactType.MERGED_RESOURCES,
                            "transformResources",
                            ExampleDirectoryTransformerTask::class.java) {
                        // some config here
                    }

                    // ------
                    // File Transform
                    holder.transform(
                            SingleFileArtifactType.MERGED_MANIFEST,
                            "transformManifest",
                            ExampleFileTransformerTask::class.java) {
                        // some config here
                    }

                    // ------
                    // Multi-File Appends & Transforms
                    // append
                    holder.append(
                            MultiFileArtifactType.JAR,
                            "generateCode1",
                            ExampleFileProducerTask::class.java
                    ) {
                        // some config here
                    }

                    // transform
                    holder.transform(
                            MultiFileArtifactType.JAR,
                            "transformCode",
                            ExampleFileListTransformerTask::class.java) {
                        // some config here
                    }

                    // append again
                    holder.append(
                            MultiFileArtifactType.JAR,
                            "generateCode2",
                            ExampleFileProducerTask::class.java) {
                        // some config here
                    }

                    // ------
                    // Multi-Directory Appends & Transforms

                    // ------
                    // Package Transform
                    holder.transform(
                            SingleFileArtifactType.PACKAGE,
                            "transformPackage",
                            ExampleFileTransformerTask::class.java) {
                        // some config here
                    }


                }
            }
        }
    }
}