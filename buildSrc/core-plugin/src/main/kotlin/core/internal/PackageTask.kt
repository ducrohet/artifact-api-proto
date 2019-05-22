package core.internal

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*

@Suppress("UnstableApiUsage")
abstract class PackageTask: DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val manifest: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val dexFiles: ListProperty<RegularFile>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedResources: DirectoryProperty

    @get:OutputFile
    abstract val outputApk: RegularFileProperty

    @TaskAction
    fun action() {
        println(name)
        println("\tmanifest: ${manifest.get().asFile}")
        for (file in dexFiles.get()) {
            println("\tdexFiles: ${file.asFile}")
        }
        println("\tmergedResources: ${mergedResources.get().asFile}")
        println("\t---")
        val outputFile = outputApk.get().asFile
        println("\tOutput: $outputFile")

        outputFile.parentFile.mkdirs()
        outputFile.writeText("foo")
    }
}