@file:Suppress("UnstableApiUsage")

package core.internal.impl

import core.api.ArtifactProducer
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import java.io.File

internal fun <TaskT, ValueT> processOutput(
        task: TaskT,
        location: File
) where TaskT : DefaultTask, TaskT : ArtifactProducer<ValueT> {
    // set output location
    when (val output = task.outputArtifact) {
        is DirectoryProperty -> output.set(location)
        is RegularFileProperty -> output.set(location)
        else -> throw RuntimeException("Unexpected output type: ${output.javaClass}")
    }

    registerOutput(task)
}

internal fun <TaskT, ValueT> processOutput(
        task: TaskT,
        location: Provider<ValueT>
) where TaskT : DefaultTask, TaskT : ArtifactProducer<ValueT> {
    task.outputArtifact.set(location)
    registerOutput(task)
}

private fun <TaskT, ValueT> registerOutput(
        task: TaskT
) where TaskT : DefaultTask, TaskT : ArtifactProducer<ValueT> {
    // register output property as a task output
    val outputBuilder = when (val output = task.outputArtifact) {
        is DirectoryProperty -> task.outputs.dir(output)
        is RegularFileProperty -> task.outputs.file(output)
        else -> throw RuntimeException("Unexpected output type: ${output.javaClass}")
    }
    outputBuilder.withPropertyName("outputArtifact")
}
