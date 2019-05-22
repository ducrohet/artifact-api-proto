@file:Suppress("UnstableApiUsage")

package core.internal.impl

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*

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


abstract class InternalFileTransformerTask : DefaultTask() {

    @get:InputFiles
    abstract val inputArtifacts: ListProperty<RegularFile>

    @get:OutputFile
    abstract val outputArtifact: RegularFileProperty

    @TaskAction
    fun action() {
        println(name)
        for (file in inputArtifacts.get()) {
            println("\tInput: ${file.asFile}")
        }
        println("\tOutput: ${outputArtifact.get().asFile}")
        outputArtifact.get().asFile.writeText("foo\n")
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

