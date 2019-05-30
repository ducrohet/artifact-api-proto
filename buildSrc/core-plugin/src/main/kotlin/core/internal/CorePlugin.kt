@file:Suppress("UnstableApiUsage")

package core.internal

import core.api.*
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

            // if present, run the lambda provided by API user(s).
            extension.block?.invoke(artifactHolder)

            // then create our own tasks
            createTasks(artifactHolder)
        }
    }

    private fun createTasks(artifactHolder: ArtifactHolderImpl) {
        createManifestMerger(artifactHolder)
        createResourceMerger(artifactHolder)
        createCompilerAndDexer(artifactHolder)
        createListDirectoryTasks(artifactHolder)

        // check if we have more than one dex, if we do then we merge the dex instead.
        // This is an example of being able to detect whether someone interacted with artifacts or not.
        var useDexMerger = false
        if (artifactHolder.hasProducer(SingleFileArtifactType.MERGED_DEX)) {
            useDexMerger = true

            // do nothing, since there's already a merged dex task
        } else if (artifactHolder.hasAppend(MultiFileArtifactType.DEX)) {

            val dexMerger = project.tasks.register(
                    "dexMerger",
                    InternalMultiToSingleFileTransformerTask::class.java) {
                it.inputArtifacts.set(artifactHolder.getArtifact(MultiFileArtifactType.DEX))
            }

            artifactHolder.produces(SingleFileArtifactType.MERGED_DEX, dexMerger, { it.outputArtifact })
            useDexMerger = true
        }

        if (!artifactHolder.hasProducer(SingleFileArtifactType.PACKAGE)) {

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
        }

        project.tasks.register("assemble") {
            it.dependsOn(artifactHolder.getArtifact(SingleFileArtifactType.PACKAGE))
        }

        artifactHolder.finalizeLocations()
    }

    private fun createResourceMerger(artifactHolder: ArtifactHolderImpl) {
        // create the original producer last
        val task = project.tasks.register(
                "resources",
                InternalDirectoryProducerTask::class.java) { }

        artifactHolder.produces(MultiDirectoryArtifactType.RESOURCES, task, { it.outputArtifact })

        val merger = project.tasks.register(
                "resourceMerger",
                InternalMultiToSingleDirectoryTransformerTask::class.java) {
            it.inputArtifacts.set(artifactHolder.getArtifact(MultiDirectoryArtifactType.RESOURCES))
        }

        artifactHolder.produces(SingleDirectoryArtifactType.MERGED_RESOURCES, merger, { it.outputArtifact })
    }

    private fun createManifestMerger(artifactHolder: ArtifactHolderImpl) {
        if (artifactHolder.hasProducer(SingleFileArtifactType.MERGED_MANIFEST).not()) {
            // create the original producer last
            val task = project.tasks.register(
                    "manifestMerger",
                    InternalFileProducerTask::class.java
            ) { }

            artifactHolder.produces(SingleFileArtifactType.MERGED_MANIFEST, task, { it.outputArtifact })
        }
    }

    private fun createListDirectoryTasks(holder: ArtifactHolderImpl) {

    }

    private fun createCompilerAndDexer(artifactHolder: ArtifactHolderImpl) {
        // create the original producer last
        val compiler = project.tasks.register(
                "compileCode",
                InternalFileProducerTask::class.java) { }

        artifactHolder.produces(MultiMixedArtifactType.BYTECODE, compiler, { it.outputArtifact })

        val dexer = project.tasks.register(
                "dexer",
                InternalMixedToSingleFileTransformerTask::class.java) {
            it.inputArtifacts.set(artifactHolder.getArtifact(MultiMixedArtifactType.BYTECODE))
        }

        artifactHolder.produces(MultiFileArtifactType.DEX, dexer, { it.outputArtifact })
    }
}