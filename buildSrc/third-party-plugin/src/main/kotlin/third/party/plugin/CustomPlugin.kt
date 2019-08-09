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
                replaceDexer(holder)
                replaceDexerAndPackage(holder)
            }
        }
    }

    private fun processPackage(holder: ArtifactHolder) {
        if (mustTransform("package")) {
            // ------
            // Output Transform
            val taskProvider = project.tasks.register("transformPackage", ExampleFileTransformerTask::class.java)

            holder.transform(SingleFileArtifactType.PACKAGE,
                    taskProvider,
                    ExampleFileTransformerTask::inputArtifact,
                    ExampleFileTransformerTask::outputArtifact)
        }
    }

    private fun processCode(holder: ArtifactHolder) {
        if (mustAdd("code")) {

            // ------
            // Mixed Appends

            // append file
            val bcAppendProvider1 = project.tasks.register(
                    "generateCode1",
                    ExampleFileProducerTask::class.java)
            holder.append(
                    MultiMixedArtifactType.BYTECODE,
                    bcAppendProvider1,
                    ExampleFileProducerTask::outputArtifact)

            // append directory
            val bcAppendProvider2 = project.tasks.register(
                    "generateCode2",
                    ExampleDirectoryProducerTask::class.java)
            holder.append(
                    MultiMixedArtifactType.BYTECODE,
                    bcAppendProvider2,
                    ExampleDirectoryProducerTask::outputArtifact)
        }

        if (mustAdd("dex")) {
            // ------
            // Multi File append
            val dexAppendProvider = project.tasks.register(
                    "generateDex",
                    ExampleFileProducerTask::class.java)

            holder.append(
                    MultiFileArtifactType.DEX,
                    dexAppendProvider,
                    ExampleFileProducerTask::outputArtifact)
        }
    }

    private fun projectManifest(holder: ArtifactHolder) {
        if (mustTransform("manifest")) {
            // ------
            // File Transform
            val taskProvider = project.tasks.register(
                    "transformManifest",
                    ExampleFileTransformerTask::class.java)

            holder.transform(SingleFileArtifactType.MERGED_MANIFEST,
                    taskProvider,
                    ExampleFileTransformerTask::inputArtifact,
                    ExampleFileTransformerTask::outputArtifact)
        }
    }

    private fun processResources(holder: ArtifactHolder) {
        if (mustTransform("resources")) {
            // ------
            // Single and multi Folder append/transform
            val resAppendProvider = project.tasks.register(
                    "generateResources",
                    ExampleDirectoryProducerTask::class.java
            )
            holder.append(
                    MultiDirectoryArtifactType.RESOURCES,
                    resAppendProvider,
                    ExampleDirectoryProducerTask::outputArtifact)

            val resTransformProvider = project.tasks.register(
                    "transformResources",
                    ExampleDirListTransformerTask::class.java)
            holder.transform(
                    MultiDirectoryArtifactType.RESOURCES,
                    resTransformProvider,
                    ExampleDirListTransformerTask::inputArtifacts,
                    ExampleDirListTransformerTask::outputArtifact)


            val resAppendProvider2 = project.tasks.register(
                    "generateMoreResources",
                    ExampleDirectoryProducerTask::class.java
            )
            holder.append(
                    MultiDirectoryArtifactType.RESOURCES,
                    resAppendProvider2,
                    ExampleDirectoryProducerTask::outputArtifact)

            val mergedResTransformProvider = project.tasks.register(
                    "transformMergedResources",
                    ExampleDirectoryTransformerTask::class.java)

            holder.transform(SingleDirectoryArtifactType.MERGED_RESOURCES,
                    mergedResTransformProvider,
                    ExampleDirectoryTransformerTask::inputArtifact,
                    ExampleDirectoryTransformerTask::outputArtifact)
        }
    }

    private fun replaceDexer(holder: ArtifactHolder) {
        if (mustReplace("dexer")) {
            holder.replace(
                    SingleFileArtifactType.MERGED_DEX,
                    "newDexer",
                    ExampleDexerTask::class.java
            ).input(MultiMixedArtifactType.BYTECODE, ExampleDexerTask::inputArtifacts
            ).finish {
                // some config
            }
        }
    }

    private fun replaceDexerAndPackage(holder: ArtifactHolder) {
        if (mustReplace("dexerAndPackager")) {
            holder.replace(
                    SingleFileArtifactType.PACKAGE,
                    "newPackager",
                    NewPackageTask::class.java
            ).input(SingleFileArtifactType.MERGED_MANIFEST, NewPackageTask::manifest
            ).input(MultiMixedArtifactType.BYTECODE, NewPackageTask::bytecodeFiles
            ).input(SingleDirectoryArtifactType.MERGED_RESOURCES, NewPackageTask::mergedResources
            ).finish {
                // some config
            }
        }

    }

    private fun mustAdd(propName: String): Boolean =
            project.properties["add.$propName"] == "true" || project.properties["add.all"] == "true"

    private fun mustTransform(propName: String): Boolean =
            project.properties["transform.$propName"] == "true" || project.properties["transform.all"] == "true"

    private fun mustReplace(propName: String): Boolean =
            project.properties["replace.$propName"] == "true" || project . properties ["replace.all"] == "true"
}