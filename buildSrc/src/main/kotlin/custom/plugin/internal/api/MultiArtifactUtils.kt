@file:Suppress("UNCHECKED_CAST")

package custom.plugin.internal.api

import custom.plugin.api.ArtifactListConsumer
import custom.plugin.api.ArtifactProducer
import custom.plugin.api.MultiArtifactType
import custom.plugin.api.MultiFileArtifactType
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import java.io.File
import java.util.*

@Suppress("UnstableApiUsage")
class MultiFileHolder(
        project: Project
): MultiArtifactHolder<MultiFileArtifactType, RegularFile, Provider<out Iterable<RegularFile>>>(project) {

    override val finalWrapperMap = EnumMap<MultiFileArtifactType, ListProperty<RegularFile>>(MultiFileArtifactType::class.java)
    override val appendWrapperMap = EnumMap<MultiFileArtifactType, ListProperty<RegularFile>>(MultiFileArtifactType::class.java)
    override val latestMap = EnumMap<MultiFileArtifactType, Provider<out Iterable<RegularFile>>>(MultiFileArtifactType::class.java)

    override fun newWrapper(): ListProperty<RegularFile> = project.objects.listProperty(RegularFile::class.java)
    override fun newLocation() = File(project.buildDir, "foo${index++}.txt")

    private var index = 2
}

@Suppress("UnstableApiUsage")
abstract class MultiArtifactHolder<ArtifactT: MultiArtifactType<ValueT, ProviderT>, ValueT, ProviderT : Provider<out Iterable<ValueT>>>(protected val project: Project) {

    protected abstract val finalWrapperMap: MutableMap<ArtifactT, ListProperty<ValueT>>
    protected abstract val appendWrapperMap: MutableMap<ArtifactT, ListProperty<ValueT>>
    protected abstract val latestMap: MutableMap<ArtifactT, Provider<out Iterable<ValueT>>>

    protected abstract fun newWrapper(): ListProperty<ValueT>

    fun getArtifact(artifactType : ArtifactT) : Provider<out Iterable<ValueT>> =
            finalWrapperMap[artifactType] ?: throw RuntimeException("Did not find artifact for $artifactType")

    fun produces(artifactType : ArtifactT, artifact: Provider<ValueT>) {
        if (finalWrapperMap.containsKey(artifactType)) {
            throw RuntimeException("singleDirMap already contains $artifactType")
        }

        val appendWrapper = newWrapper()
        appendWrapper.add(artifact)
        appendWrapperMap[artifactType] = appendWrapper

        val finalWrapper = newWrapper()
        finalWrapper.set(appendWrapper)
        finalWrapperMap[artifactType] = finalWrapper

        latestMap[artifactType] = appendWrapper
    }

    fun <TaskT> transform(
            artifactType : ArtifactT,
            taskName: String,
            taskClass: Class<TaskT>,
            configAction: (TaskT) -> Unit
    ): TaskProvider<TaskT> where TaskT: DefaultTask, TaskT: ArtifactListConsumer<ValueT>, TaskT: ArtifactProducer<ValueT> {

        val newTask = project.tasks.register(taskName, taskClass)

        val wrapper = finalWrapperMap[artifactType] ?: throw RuntimeException("Did not find artifact for $artifactType")
        val previousLatest = latestMap[artifactType]?: throw RuntimeException("Did not find artifact for $artifactType")

        // create a new property?
        val newLatest = newWrapper().also { w -> w.add(newTask.flatMap { it.outputArtifact })}

        // update wrapper and currentArtifact

        wrapper.set(newLatest)
        latestMap[artifactType] = newLatest

        newTask.configure {
            // set input and register input with sensitivity
            it.inputArtifacts.set(previousLatest)
            val inputBuilder = it.inputs
                    .files(it.inputArtifacts)
                    .withPropertyName("inputArtifacts")
                    .withPathSensitivity(artifactType.sensitivity)
            artifactType.normalizer?.let { normalizer ->
                inputBuilder.withNormalizer(normalizer)
            }

            // set output location and register output
            processOutput(it, newLocation())

            // run the user's configuration action
            configAction.invoke(it)
        }

        return newTask
    }

    fun <TaskT> append(
            artifactType : ArtifactT,
            taskName: String,
            taskClass: Class<TaskT>,
            configAction: (TaskT) -> Unit
    ): TaskProvider<TaskT> where TaskT: DefaultTask, TaskT: ArtifactProducer<ValueT> {

        val newTask = project.tasks.register(taskName, taskClass)

        val wrapper = appendWrapperMap[artifactType] ?: throw RuntimeException("Did not find artifact for $artifactType")

        // get property to add
        val newOutput = newTask.flatMap { it.outputArtifact }

        wrapper.add(newOutput)

        newTask.configure {
            // set output location and register output
            processOutput(it, newLocation())
            // run the user's configuration action
            configAction.invoke(it)
        }

        return newTask
    }


    protected abstract fun newLocation(): File
}


@Suppress("UnstableApiUsage")
internal fun <TaskT, ValueT> processOutput(task: TaskT, location: File? = null) where TaskT : DefaultTask, TaskT : ArtifactProducer<ValueT> {
    // set output location and register output
    val outputBuilder = when (val output = task.outputArtifact) {
        is DirectoryProperty -> {
            if (location != null) output.set(location)
            task.outputs.dir(output)
        }
        is RegularFileProperty -> {
            if (location != null) output.set(location)
            task.outputs.file(output)
        }
        else -> {
            throw RuntimeException("Unexpected output type: ${output.javaClass}")
        }
    }
    outputBuilder.withPropertyName("outputArtifact")
}
