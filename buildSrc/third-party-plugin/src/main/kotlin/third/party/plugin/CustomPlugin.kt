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
                    // Single and multi Folder append/transform
                    holder.append(
                            MultiDirectoryArtifactType.RESOURCES,
                            "generateResources",
                            ExampleDirectoryProducerTask::class.java) {
                        // some config here
                    }
                    holder.transform(
                            MultiDirectoryArtifactType.RESOURCES,
                            "transformResources",
                            ExampleDirListTransformerTask::class.java) {
                        // some config here
                    }
                    holder.append(
                            MultiDirectoryArtifactType.RESOURCES,
                            "generateMoreResources",
                            ExampleDirectoryProducerTask::class.java) {
                        // some config here
                    }

                    holder.transform(
                            SingleDirectoryArtifactType.MERGED_RESOURCES,
                            "transformMergedResources",
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
                    // Mixed Appends

                    // append file
                    holder.append(
                            MultiMixedArtifactType.BYTECODE,
                            "generateCode1",
                            ExampleFileProducerTask::class.java
                    ) {
                        // some config here
                    }

                    // append directory
                    holder.append(
                            MultiMixedArtifactType.BYTECODE,
                            "generateCode2",
                            ExampleDirectoryProducerTask::class.java) {
                        // some config here
                    }

                    // ------
                    // Multi File append
                    holder.append(
                            MultiFileArtifactType.DEX,
                            "generateDex",
                            ExampleFileProducerTask::class.java) {
                        // some config here
                    }

                    // ------
                    // Multi-Directory Appends & Transforms

                    // ------
                    // Output Transform
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