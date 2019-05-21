package custom.plugin.internal.plugin

import custom.plugin.api.ArtifactHolder
import custom.plugin.api.FileConsumerTask
import custom.plugin.api.FileProducerTask
import custom.plugin.api.SingleFileArtifactType
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import java.io.File

/**
 * Simple class creating tasks that produce, consume and transform files
 */
@Suppress("UnstableApiUsage")
class FileTaskCreator(private val project: Project, private val artifactHolder: ArtifactHolder) {

    fun create() {
        // create transform first
        if (project.properties["android.transform"] == "true") {
            createTransform()
        }

        // create final consumer
        project.tasks.register("finalFile", InternalFileConsumerTask::class.java) {
            it.inputArtifact.set(artifactHolder.getArtifact(SingleFileArtifactType.MANIFEST))
        }

        // create the original producer last
        val originTask = project.tasks.register("originFile", InternalFileProducerTask::class.java) {
            it.outputArtifact.set(File(project.buildDir, "foo.txt"))
        }

        artifactHolder.produces(SingleFileArtifactType.MANIFEST, originTask.flatMap { it.outputArtifact })
    }

    private fun createTransform() {
        artifactHolder.transform(
                SingleFileArtifactType.MANIFEST,
                "transformFile",
                ExampleFileTransformerTask::class.java) {
            // some config here
        }
    }
}

@Suppress("UnstableApiUsage")
abstract class InternalFileConsumerTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputArtifact: RegularFileProperty

    @TaskAction
    fun action() {
        println(name)
        println("\tInput: ${inputArtifact.get().asFile}")
    }
}

@Suppress("UnstableApiUsage")
abstract class InternalFileProducerTask : DefaultTask() {

    @get:OutputFile
    abstract val outputArtifact: RegularFileProperty

    @TaskAction
    fun action() {
        println(name)
        println("\tOutput: ${outputArtifact.get().asFile}")
        outputArtifact.get().asFile.writeText("foo\n")
    }
}



@Suppress("UnstableApiUsage")
abstract class ExampleFileProducerTask : DefaultTask(), FileProducerTask {

    @TaskAction
    fun action() {
        println(name)
        println("\tOutput: ${outputArtifact.get().asFile}")
        outputArtifact.get().asFile.writeText("foo\n")
    }
}

@Suppress("UnstableApiUsage")
abstract class ExampleFileTransformerTask: DefaultTask(), FileProducerTask, FileConsumerTask {

    @TaskAction
    fun action() {
        println(name)
        println("\tInput: ${inputArtifact.get().asFile}")
        println("\tOutput: ${outputArtifact.get().asFile}")
        outputArtifact.get().asFile.writeText(inputArtifact.get().asFile.readText() + "new content\n")
    }
}
