@file:Suppress("UnstableApiUsage")

package core.internal

import core.api.MultiFileArtifactType
import core.api.SingleDirectoryArtifactType
import core.api.SingleFileArtifactType
import core.internal.impl.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

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
        createDirectoryTasks(artifactHolder)
        createFileTasks(artifactHolder)
        createListDirectoryTasks(artifactHolder)
        createListFileTasks(artifactHolder)
    }

    private fun createDirectoryTasks(artifactHolder: ArtifactHolderImpl) {
        // create final consumer
        project.tasks.register("finalDir", InternalDirectoryConsumerTask::class.java) {
            it.inputArtifact.set(artifactHolder.getArtifact(SingleDirectoryArtifactType.MERGED_RESOURCES))
        }

        // create the original producer last
        val originTask = project.tasks.register(
                "originDir",
                InternalDirectoryProducerTask::class.java) {
            it.outputArtifact.set(File(project.buildDir, "foo"))
        }

        artifactHolder.produces(SingleDirectoryArtifactType.MERGED_RESOURCES, originTask.flatMap { it.outputArtifact })
    }

    private fun createFileTasks(artifactHolder: ArtifactHolderImpl) {
        // create final consumer
        project.tasks.register(
                "finalFile",
                InternalFileConsumerTask::class.java
        ) { task ->
            task.inputArtifact.set(artifactHolder.getArtifact(SingleFileArtifactType.MANIFEST))
        }

        // create the original producer last
        val originTask = project.tasks.register(
                "originFile",
                InternalFileProducerTask::class.java
        ) {
            task -> task.outputArtifact.set(File(project.buildDir, "foo.txt"))
        }

        artifactHolder.produces(SingleFileArtifactType.MANIFEST, originTask.flatMap { it.outputArtifact })
    }

    private fun createListDirectoryTasks(artifactHolder: ArtifactHolderImpl) {

    }

    private fun createListFileTasks(artifactHolder: ArtifactHolderImpl) {
        // create final consumer
        project.tasks.register("finalFileList", InternalFileListConsumerTask::class.java) {
            it.inputArtifacts.set(artifactHolder.getArtifact(MultiFileArtifactType.DEX))
        }

        // create the original producer last
        val originTask = project.tasks.register(
                "originFileList",
                InternalFileProducerTask::class.java) {
            it.outputArtifact.set(File(project.buildDir, "foo.txt"))
        }

        artifactHolder.produces(MultiFileArtifactType.DEX, originTask.flatMap { it.outputArtifact })
    }

}