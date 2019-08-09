@file:Suppress("UnstableApiUsage")

package third.party.plugin

import core.api.*
import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*
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

abstract class ExampleFileTransformerTask: DefaultTask() {

    @get:InputFile
    abstract val inputArtifact: RegularFileProperty

    @get:OutputFile
    abstract val outputArtifact: RegularFileProperty

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

    @get:Internal
    abstract val inputArtifacts: ListProperty<FileSystemLocation>

    @TaskAction
    fun action() {
        println("$name(CustomPlugin)")
        var index = 1
        for (file in inputArtifacts.get()) {
            println("\tInput${index++}: ${file.asFile} (${getType(file)})")
        }
        println("\t---")
        println("\tOutput: ${outputArtifact.get().asFile}")
        outputArtifact.get().asFile.writeText("foo\n")
    }
}

@Suppress("UnstableApiUsage")
abstract class NewPackageTask: DefaultTask(), FileProducerTask {

    abstract val manifest: RegularFileProperty

    abstract val bytecodeFiles: ListProperty<FileSystemLocation>

    abstract val mergedResources: DirectoryProperty

    @TaskAction
    fun action() {
        println("$name(CustomPlugin)")
        println("\tmanifest: ${manifest.get().asFile}")
        var index = 1
        for (file in bytecodeFiles.get()) {
            println("\tbytecodeFiles${index++}: ${file.asFile} (${getType(file)})")
        }
        println("\tmergedResources: ${mergedResources.get().asFile}")

        println("\t---")

        val outputFile = outputArtifact.get().asFile
        println("\tOutput: $outputFile")
        outputFile.parentFile.mkdirs()
        outputFile.writeText("foo")
    }
}


@Suppress("UnstableApiUsage")
private fun getType(file: FileSystemLocation): String {
    return when(file) {
        is Directory -> "Directory"
        is RegularFile -> "File"
        else -> throw RuntimeException("unsupported FileSystemLocation: ${file.javaClass}")
    }
}
