package third.party.plugin

import core.api.*
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File


abstract class ExampleDirectoryTransformerTask: DefaultTask(), DirectoryProducerTask, DirectoryConsumerTask {

    @TaskAction
    fun action() {
        println("$name(CustomPlugin)")
        println("\tInput: ${inputArtifact.get().asFile}")
        println("\t---")
        println("\tOutput: ${outputArtifact.get().asFile}")
    }
}

abstract class ExampleFileProducerTask : DefaultTask(), FileProducerTask {

    @TaskAction
    fun action() {
        println("$name(CustomPlugin)")
        println("\tOutput: ${outputArtifact.get().asFile}")
        outputArtifact.get().asFile.writeText("foo\n")
    }
}

abstract class ExampleDirectoryProducerTask : DefaultTask(), DirectoryProducerTask {

    @TaskAction
    fun action() {
        println("$name(CustomPlugin)")
        val folder = outputArtifact.get().asFile
        println("\tOutput: $folder")
        folder.mkdirs()
        File(folder, "foo.txt").writeText("foo\n")
    }
}

abstract class ExampleFileTransformerTask: DefaultTask(), FileProducerTask, FileConsumerTask {

    @TaskAction
    fun action() {
        println("$name(CustomPlugin)")
        println("\tInput: ${inputArtifact.get().asFile}")
        println("\t---")
        println("\tOutput: ${outputArtifact.get().asFile}")
        outputArtifact.get().asFile.writeText(inputArtifact.get().asFile.readText() + "new content\n")
    }
}

abstract class ExampleFileListTransformerTask: DefaultTask(), FileListConsumerTask, FileProducerTask {

    @TaskAction
    fun action() {
        println("$name(CustomPlugin)")
        var index = 1
        for (file in inputArtifacts.get()) {
            println("\tInput${index++}: ${file.asFile}")
        }
        println("\t---")
        println("\tOutput: ${outputArtifact.get().asFile}")
        outputArtifact.get().asFile.writeText("foo2\n")
    }
}

abstract class ExampleDirListTransformerTask: DefaultTask(), DirectoryListConsumerTask, DirectoryProducerTask {

    @TaskAction
    fun action() {
        println("$name(CustomPlugin)")
        var index = 1
        for (file in inputArtifacts.get()) {
            println("\tInput${index++}: ${file.asFile}")
        }
        println("\t---")
        val outputFolder = outputArtifact.get().asFile
        println("\tOutput: $outputFolder")
        File(outputFolder, "foo.txt").writeText("foo2\n")
    }
}
