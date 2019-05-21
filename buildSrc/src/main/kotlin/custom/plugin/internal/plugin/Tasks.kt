package custom.plugin.internal.plugin

import custom.plugin.api.FileConsumerTask
import custom.plugin.api.FileProducerTask
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*


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
