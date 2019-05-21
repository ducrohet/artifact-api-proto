package custom.plugin.internal.plugin

import custom.plugin.api.ArtifactHolder
import custom.plugin.api.DirectoryConsumerTask
import custom.plugin.api.DirectoryProducerTask
import custom.plugin.api.SingleDirectoryArtifactType
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.*
import java.io.File

/**
 * Simple class creating tasks that produce, consume and transform directories
 */
@Suppress("UnstableApiUsage")
class DirTaskCreator(private val project: Project, private val artifactHolder: ArtifactHolder) {

    fun create() {
        // create transform first
        if (project.properties["android.transform"] == "true") {
            createTransform()
        }

        // create final consumer
        project.tasks.register("finalDir", InternalDirectoryConsumerTask::class.java) {
            it.inputArtifact.set(artifactHolder.getArtifact(SingleDirectoryArtifactType.MERGED_RESOURCES))
        }

        // create the original producer last
        val originTask = project.tasks.register("originDir", InternalDirectoryProducerTask::class.java) {
            it.outputArtifact.set(File(project.buildDir, "foo"))
        }

        artifactHolder.produces(SingleDirectoryArtifactType.MERGED_RESOURCES, originTask.flatMap { it.outputArtifact })
    }

    private fun createTransform() {
        artifactHolder.transform(
                SingleDirectoryArtifactType.MERGED_RESOURCES,
                "transformDir",
                ExampleDirectoryTransformerTask::class.java) {
            // some config here
        }
    }
}

@Suppress("UnstableApiUsage")
abstract class InternalDirectoryProducerTask : DefaultTask() {

    @get:OutputDirectory
    abstract val outputArtifact: DirectoryProperty

    @TaskAction
    fun action() {
        println(name)
        println("\tOutput: ${outputArtifact.get().asFile}")
    }
}

@Suppress("UnstableApiUsage")
abstract class InternalDirectoryConsumerTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputArtifact: DirectoryProperty

    @TaskAction
    fun action() {
        println(name)
        println("\tInput: ${inputArtifact.get().asFile}")
    }
}

@Suppress("UnstableApiUsage")
abstract class ExampleDirectoryTransformerTask: DefaultTask(), DirectoryProducerTask, DirectoryConsumerTask {

    @TaskAction
    fun action() {
        println(name)
        println("\tInput: ${inputArtifact.get().asFile}")
        println("\tOutput: ${outputArtifact.get().asFile}")
    }
}
