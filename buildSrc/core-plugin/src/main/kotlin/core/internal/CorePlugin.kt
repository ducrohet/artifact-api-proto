@file:Suppress("UnstableApiUsage")

package core.internal

import core.api.MultiFileArtifactType
import core.api.SingleDirectoryArtifactType
import core.api.SingleFileArtifactType
import core.internal.impl.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFile

class CorePlugin: Plugin<Project> {

    private lateinit var project: Project

    override fun apply(project: Project) {
        this.project = project
        val extension = project.extensions.create("artifacts", ExtensionImpl::class.java)

        project.afterEvaluate {
            val artifactHolder = ArtifactHolderImpl(project)
            extension.block?.invoke(artifactHolder)

            createTasks(artifactHolder)
        }
    }

    private fun createTasks(artifactHolder: ArtifactHolderImpl) {
        createManifestMerger(artifactHolder)
        createResourceMerger(artifactHolder)
        createCompilerAndDexer(artifactHolder)
        createListDirectoryTasks(artifactHolder)

        // check if we have more than one dex, if we do then we merge the dex instead.
        var useDexMerger = false
        if (artifactHolder.hasAppend(MultiFileArtifactType.DEX)) {

            val dexMerger = project.tasks.register(
                    "dexMerger",
                    InternalMultiToSingleFileTransformerTask::class.java) {
                it.inputArtifacts.set(artifactHolder.getArtifact(MultiFileArtifactType.DEX))
            }

            artifactHolder.produces(SingleFileArtifactType.MERGED_DEX, dexMerger, { it.outputArtifact })
            useDexMerger = true
        }

        val packageTask = project.tasks.register("packageApk", PackageTask::class.java) {
            it.manifest.set(artifactHolder.getArtifact(SingleFileArtifactType.MERGED_MANIFEST))
            it.mergedResources.set(artifactHolder.getArtifact(SingleDirectoryArtifactType.MERGED_RESOURCES))
            if (useDexMerger) {
                val wrapper = project.objects.listProperty(RegularFile::class.java)
                wrapper.add(artifactHolder.getArtifact(SingleFileArtifactType.MERGED_DEX))
                it.dexFiles.set(wrapper)
            } else {
                it.dexFiles.set(artifactHolder.getArtifact(MultiFileArtifactType.DEX))
            }
        }

        artifactHolder.produces(SingleFileArtifactType.PACKAGE, packageTask, { it.outputApk })


        project.tasks.register("assemble") {
            it.dependsOn(artifactHolder.getArtifact(SingleFileArtifactType.PACKAGE))
        }

        artifactHolder.finalizeLocations()
    }

    private fun createResourceMerger(artifactHolder: ArtifactHolderImpl) {
        // create the original producer last
        val task = project.tasks.register(
                "resourceMerger",
                InternalDirectoryProducerTask::class.java) { }

        artifactHolder.produces(SingleDirectoryArtifactType.MERGED_RESOURCES, task, { it.outputArtifact })
    }

    private fun createManifestMerger(artifactHolder: ArtifactHolderImpl) {
        // create the original producer last
        val task = project.tasks.register(
                "manifestMerger",
                InternalFileProducerTask::class.java
        ) { }

        artifactHolder.produces(SingleFileArtifactType.MERGED_MANIFEST, task, { it.outputArtifact })
    }

    private fun createListDirectoryTasks(artifactHolder: ArtifactHolderImpl) {

    }

    private fun createCompilerAndDexer(artifactHolder: ArtifactHolderImpl) {
        // create the original producer last
        val compiler = project.tasks.register(
                "compileCode",
                InternalFileProducerTask::class.java) {
        }

        artifactHolder.produces(MultiFileArtifactType.JAR, compiler, { it.outputArtifact })

        val dexer = project.tasks.register(
                "dexer",
                InternalMultiToSingleFileTransformerTask::class.java) {
            it.inputArtifacts.set(artifactHolder.getArtifact(MultiFileArtifactType.JAR))
        }

        artifactHolder.produces(MultiFileArtifactType.DEX, dexer, { it.outputArtifact })
    }
}