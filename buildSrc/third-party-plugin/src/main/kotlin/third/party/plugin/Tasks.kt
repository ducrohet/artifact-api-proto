package third.party.plugin

import core.api.*
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.lang.RuntimeException


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

@Suppress("UnstableApiUsage")
abstract class ExampleDexerTask: DefaultTask(), FileProducerTask {

    @get:InputFiles
    abstract val inputArtifacts: ListProperty<FileSystemLocation>

    @TaskAction
    fun action() {
        println(name)
        var index = 1
        for (file in inputArtifacts.get()) {
            println("\tInput${index++}: ${file.asFile} (${getType(file)})")
        }
        println("\t---")
        println("\tOutput: ${outputArtifact.get().asFile}")
        outputArtifact.get().asFile.writeText("foo\n")
    }

    private fun getType(file: FileSystemLocation): String {
        return when(file) {
            is Directory -> "Directory"
            is RegularFile -> "File"
            else -> throw RuntimeException("unsupported FileSystemLocation: ${file.javaClass}")
        }
    }
}
