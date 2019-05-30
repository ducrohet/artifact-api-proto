@file:Suppress("UnstableApiUsage")

package core.internal.impl

import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*
import java.io.File
import java.lang.RuntimeException

abstract class InternalDirectoryProducerTask : DefaultTask() {

    @get:OutputDirectory
    abstract val outputArtifact: DirectoryProperty

    @TaskAction
    fun action() {
        println(name)
        println("\tOutput: ${outputArtifact.get().asFile}")
    }
}

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


abstract class InternalMultiToSingleFileTransformerTask : DefaultTask() {

    @get:InputFiles
    abstract val inputArtifacts: ListProperty<RegularFile>

    @get:OutputFile
    abstract val outputArtifact: RegularFileProperty

    @TaskAction
    fun action() {
        println(name)
        var index = 1
        for (file in inputArtifacts.get()) {
            println("\tInput${index++}: ${file.asFile}")
        }
        println("\t---")
        println("\tOutput: ${outputArtifact.get().asFile}")
        outputArtifact.get().asFile.writeText("foo\n")
    }
}

abstract class InternalMultiToSingleDirectoryTransformerTask : DefaultTask() {

    @get:InputFiles
    abstract val inputArtifacts: ListProperty<Directory>

    @get:OutputDirectory
    abstract val outputArtifact: DirectoryProperty

    @TaskAction
    fun action() {
        println(name)
        var index = 1
        for (file in inputArtifacts.get()) {
            println("\tInput${index++}: ${file.asFile}")
        }
        println("\t---")
        val folder = outputArtifact.get().asFile
        println("\tOutput: $folder")
        folder.mkdirs()
        File(folder, "foo.txt").writeText("foo\n")
    }
}

abstract class InternalMixedToSingleFileTransformerTask : DefaultTask() {

    @get:InputFiles
    abstract val inputArtifacts: ListProperty<FileSystemLocation>

    @get:OutputFile
    abstract val outputArtifact: RegularFileProperty

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

@Suppress("UnstableApiUsage")
abstract class InternalFileListConsumerTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputArtifacts: ListProperty<RegularFile>

    @TaskAction
    fun action() {
        println(name)
        var index = 1
        for (file in inputArtifacts.get()) {
            println("\tInput${index++}: ${file.asFile}")
        }
    }
}

