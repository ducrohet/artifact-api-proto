@file:Suppress("UnstableApiUsage")

package third.party.plugin

import core.api.*
import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*
import java.io.File
import java.lang.RuntimeException


abstract class ExampleDirectoryTransformerTask: DefaultTask() {

    @get:InputDirectory
    abstract val inputArtifact: DirectoryProperty

    @get:OutputDirectory
    abstract val outputArtifact: DirectoryProperty

    @TaskAction
    fun action() {
        println("$name(CustomPlugin)")
        println("\tInput: ${inputArtifact.get().asFile}")
        println("\t---")
        println("\tOutput: ${outputArtifact.get().asFile}")
    }
}

abstract class ExampleFileProducerTask : DefaultTask() {

    @get:OutputFile
    abstract val outputArtifact: RegularFileProperty

    @TaskAction
    fun action() {
        println("$name(CustomPlugin)")
        println("\tOutput: ${outputArtifact.get().asFile}")
        outputArtifact.get().asFile.writeText("foo\n")
    }
}

abstract class ExampleDirectoryProducerTask : DefaultTask() {

    @get:OutputDirectory
    abstract val outputArtifact: DirectoryProperty

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

abstract class ExampleFileListTransformerTask: DefaultTask() {

    @get:InputFiles
    abstract val inputArtifacts: ListProperty<RegularFile>

    @get:OutputFile
    abstract val outputArtifact: RegularFileProperty

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

abstract class ExampleDirListTransformerTask: DefaultTask() {

    @get:InputFiles
    abstract val inputArtifacts: ListProperty<Directory>

    @get:OutputDirectory
    abstract val outputArtifact: DirectoryProperty

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
abstract class ExampleDexerTask: DefaultTask() {

    @get:InputFiles
    abstract val inputArtifacts: ListProperty<FileSystemLocation>

    @get:OutputFile
    abstract val outputArtifact: RegularFileProperty

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
abstract class NewPackageTask: DefaultTask() {

    @get:InputFile
    abstract val manifest: RegularFileProperty

    @get:InputFiles
    abstract val bytecodeFiles: ListProperty<FileSystemLocation>

    @get:InputDirectory
    abstract val mergedResources: DirectoryProperty

    @get:OutputFile
    abstract val outputArtifact: RegularFileProperty

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
