package third.party.plugin

import core.api.*
import org.gradle.api.Plugin
import org.gradle.api.Project

class CustomPlugin: Plugin<Project> {

    private lateinit var project: Project

    override fun apply(project: Project) {
        this.project = project
        project.plugins.withType(PublicPlugin::class.java) {
            val extension = project.extensions.getByType(Extension::class.java)

            extension.handleArtifacts { holder ->
                processResources(holder)
                projectManifest(holder)
                processCode(holder)
                processPackage(holder)
                processDex(holder)
            }
        }
    }

    private fun processDex(holder: ArtifactHolder) {
        if (mustReplace("dexer")) {
            holder.replace(
                    SingleFileArtifactType.MERGED_DEX,
                    "newDexer",
                    ExampleDexerTask::class.java
            ) {
                it.inputArtifacts.set(holder.getArtifact(MultiMixedArtifactType.BYTECODE))
            }
        }
    }

    private fun processPackage(holder: ArtifactHolder) {
        if (mustTransform("package")) {
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

    private fun processCode(holder: ArtifactHolder) {
        if (mustTransform("code")) {
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
        }
    }

    private fun projectManifest(holder: ArtifactHolder) {
        if (mustTransform("manifest")) {
            // ------
            // File Transform
            holder.transform(
                    SingleFileArtifactType.MERGED_MANIFEST,
                    "transformManifest",
                    ExampleFileTransformerTask::class.java) {
                // some config here
            }
        }
    }

    private fun processResources(holder: ArtifactHolder) {
        if (mustTransform("resources")) {
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
        }
    }

    private fun mustTransform(propName: String): Boolean =
            project.properties[propName] == "true" || project.properties["transform.all"] == "true"

    private fun mustReplace(propName: String): Boolean = project.properties["replace.$propName"] == "true"

}