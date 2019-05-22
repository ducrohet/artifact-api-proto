@file:Suppress("UNCHECKED_CAST", "UnstableApiUsage")
package custom.plugin.internal.api

import custom.plugin.api.*
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

class MultiFileHolder(
        project: Project
): MultiArtifactHolder<MultiFileArtifactType, RegularFile, Provider<out Iterable<RegularFile>>>(project) {

    override val map = EnumMap<MultiFileArtifactType, MultiArtifactInfo<RegularFile>>(MultiFileArtifactType::class.java)

    override fun newListProperty(): ListProperty<RegularFile> = project.objects.listProperty(RegularFile::class.java)
    override fun newLocation() = File(project.buildDir, "foo${index++}.txt")

    private var index = 2

    init {
        for (artifact in MultiFileArtifactType.values()) {
            init(artifact)
        }
    }
}

class MultiArtifactInfo<ValueT>(
        val finalArtifact: ListProperty<ValueT>,
        currentArtifactProperty: ListProperty<ValueT>,
        private val firstArtifact: ListProperty<ValueT>
) {
    var currentArtifact: Provider<out Iterable<ValueT>>

    fun append(artifact: Provider<ValueT>) {
        firstArtifact.add(artifact)
    }

    init {
        currentArtifactProperty.set(firstArtifact)
        currentArtifact = currentArtifactProperty
        finalArtifact.set(currentArtifact)
    }
}

abstract class MultiArtifactHolder<ArtifactT: MultiArtifactType<ValueT, ProviderT>, ValueT, ProviderT : Provider<out Iterable<ValueT>>>(protected val project: Project) {

    protected abstract val map: MutableMap<ArtifactT, MultiArtifactInfo<ValueT>>

    protected abstract fun newListProperty(): ListProperty<ValueT>

    protected fun init(artifactType: ArtifactT) {
        map[artifactType] = MultiArtifactInfo(newListProperty(), newListProperty(), newListProperty())
    }

    fun getArtifact(artifactType : ArtifactT) : Provider<out Iterable<ValueT>> =
            map[artifactType]?.finalArtifact ?: throw RuntimeException("Did not find artifact for $artifactType")

    fun produces(artifactType : ArtifactT, artifact: Provider<ValueT>) {
        val info = map[artifactType] ?: throw RuntimeException("Did not find artifact for $artifactType")

        info.append(artifact)
    }

    fun <TaskT> transform(
            artifactType : ArtifactT,
            taskName: String,
            taskClass: Class<TaskT>,
            configAction: (TaskT) -> Unit
    ): TaskProvider<TaskT> where TaskT: DefaultTask, TaskT: ArtifactListConsumer<ValueT>, TaskT: ArtifactProducer<ValueT> {
        val info = map[artifactType] ?: throw RuntimeException("Did not find artifact for $artifactType")

        val previousCurrent = info.currentArtifact

        val newTask = project.tasks.register(taskName, taskClass)

        // create a new property?
        val newCurrent = newListProperty().also { w -> w.add(newTask.flatMap { it.outputArtifact })}

        // update finalArtifact and currentArtifact
        info.finalArtifact.set(newCurrent)
        info.currentArtifact = newCurrent

        newTask.configure {
            // set input and register input with sensitivity
            it.inputArtifacts.set(previousCurrent)
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
        val info = map[artifactType] ?: throw RuntimeException("Did not find artifact for $artifactType")

        val newTask = project.tasks.register(taskName, taskClass)

        // append the task output
        info.append(newTask.flatMap { it.outputArtifact })

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
