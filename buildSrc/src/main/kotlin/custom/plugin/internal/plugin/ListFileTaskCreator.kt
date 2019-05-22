package custom.plugin.internal.plugin

import custom.plugin.api.ArtifactHolder
import custom.plugin.api.FileListConsumerTask
import custom.plugin.api.FileProducerTask
import custom.plugin.api.MultiFileArtifactType
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*
import java.io.File

/**
 * Simple class creating tasks that produce, consume and transform list of files
 */
@Suppress("UnstableApiUsage")
class ListFileTaskCreator(private val project: Project, private val artifactHolder: ArtifactHolder) {

    fun create() {
        // create transform first
        if (project.properties["android.transform"] == "true") {
            createTransformAndAppend()
        }

        // create final consumer
        project.tasks.register("finalFileList", InternalFileListConsumerTask::class.java) {
            it.inputArtifacts.set(artifactHolder.getArtifact(MultiFileArtifactType.DEX))
        }

        // create the original producer last
        val originTask = project.tasks.register("originFileList", InternalFileProducerTask::class.java) {
            it.outputArtifact.set(File(project.buildDir, "foo.txt"))
        }

        artifactHolder.produces(MultiFileArtifactType.DEX, originTask.flatMap { it.outputArtifact })
    }

    private fun createTransformAndAppend() {
        // append
        artifactHolder.append(
                MultiFileArtifactType.DEX,
                "appendFile1",
                ExampleFileProducerTask::class.java
        ) {
            // some config here
        }

        // transform
        artifactHolder.transform(
                MultiFileArtifactType.DEX,
                "transformFileList",
                ExampleFileListTransformerTask::class.java) {
            // some config here
        }

        // append again
        artifactHolder.append(
                MultiFileArtifactType.DEX,
                "appendFile2",
                ExampleFileProducerTask::class.java) {
            // some config here
        }
    }
}

@Suppress("UnstableApiUsage")
abstract class InternalFileListConsumerTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputArtifacts: ListProperty<RegularFile>

    @TaskAction
    fun action() {
        println(name)
        for (file in inputArtifacts.get()) {
            println("\tInput: ${file.asFile}")
        }
    }
}

@Suppress("UnstableApiUsage")
abstract class ExampleFileListTransformerTask: DefaultTask(), FileListConsumerTask, FileProducerTask {

    @TaskAction
    fun action() {
        println(name)
        for (file in inputArtifacts.get()) {
            println("\tInput: ${file.asFile}")
        }
        println("\tOutput: ${outputArtifact.get().asFile}")
        outputArtifact.get().asFile.writeText("foo2\n")
    }
}
