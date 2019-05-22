@file:Suppress("UNCHECKED_CAST", "UnstableApiUsage")
package core.internal.impl

import core.api.*
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import java.io.File
import java.nio.file.FileSystem
import java.util.*

class MultiFileHolder(
        project: Project
): MultiArtifactHolder<MultiFileArtifactType, RegularFile, Provider<out Iterable<RegularFile>>>(project) {

    override val map = EnumMap<MultiFileArtifactType, MultiArtifactInfo<RegularFile>>(MultiFileArtifactType::class.java)

    override fun newListProperty(): ListProperty<RegularFile> = project.objects.listProperty(RegularFile::class.java)

    override fun newOutputLocation(artifactType: MultiFileArtifactType, taskName: String): Property<RegularFile> =
            project.objects.fileProperty().also {
                it.set(newLocation(artifactType, taskName))
            }

    override fun newLocation(artifactType: MultiFileArtifactType, taskName: String): File =
            File(project.buildDir, "intermediates/$taskName/$artifactType.txt")

    init {
        for (artifact in MultiFileArtifactType.values()) {
            init(artifact)
        }
    }
}

class MultiArtifactInfo<ValueT: FileSystemLocation>(
        val finalArtifact: ListProperty<ValueT>,
        currentArtifactProperty: ListProperty<ValueT>,
        private val firstArtifact: ListProperty<ValueT>
) {
    var currentArtifact: Provider<out Iterable<ValueT>>

    lateinit var finalArtifactLocation: Provider<ValueT>

    fun append(artifact: Provider<ValueT>) {
        firstArtifact.add(artifact)
    }

    init {
        currentArtifactProperty.set(firstArtifact)
        currentArtifact = currentArtifactProperty
        finalArtifact.set(currentArtifact)
    }
}

abstract class MultiArtifactHolder<ArtifactT: MultiArtifactType<ValueT, ProviderT>, ValueT: FileSystemLocation, ProviderT : Provider<out Iterable<ValueT>>>(
        protected val project: Project
) {

    protected abstract val map: MutableMap<ArtifactT, MultiArtifactInfo<ValueT>>

    protected abstract fun newListProperty(): ListProperty<ValueT>
    protected abstract fun newOutputLocation(artifactType: ArtifactT, taskName: String): Property<ValueT>
    protected abstract fun newLocation(artifactType: ArtifactT, taskName: String): File

    protected fun init(artifactType: ArtifactT) {
        map[artifactType] = MultiArtifactInfo(newListProperty(), newListProperty(), newListProperty())
    }

    fun getArtifact(artifactType : ArtifactT) : Provider<out Iterable<ValueT>> =
            map[artifactType]?.finalArtifact ?: throw RuntimeException("Did not find artifact for $artifactType")

    fun <TaskT: DefaultTask> produces(
            artifactType: ArtifactT,
            taskProvider: TaskProvider<TaskT>,
            outputProvider: (TaskT) -> Property<ValueT>
    ) {
        val info = map[artifactType] ?: throw RuntimeException("Did not find artifact for $artifactType")

        info.append(taskProvider.flatMap { outputProvider(it) })

        taskProvider.configure {
            when (val output = outputProvider(it)) {
                is DirectoryProperty -> output.set(newLocation(artifactType, it.name))
                is RegularFileProperty -> output.set(newLocation(artifactType, it.name))
            }
        }
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

        val locationAsProperty = newOutputLocation(artifactType, taskName)
        info.finalArtifactLocation = locationAsProperty

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
            processOutput(it, locationAsProperty)

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
            processOutput(it, newLocation(artifactType, it.name))
            // run the user's configuration action
            configAction.invoke(it)
        }

        return newTask
    }
}


internal fun <TaskT, ValueT> processOutput(task: TaskT, location: File? = null) where TaskT : DefaultTask, TaskT : ArtifactProducer<ValueT> {
    // set output location
    if (location != null) {
        when (val output = task.outputArtifact) {
            is DirectoryProperty -> output.set(location)
            is RegularFileProperty -> output.set(location)
            else -> throw RuntimeException("Unexpected output type: ${output.javaClass}")
        }
    }

    registerOutput(task)
}

private fun <TaskT, ValueT> registerOutput(task: TaskT) where TaskT : DefaultTask, TaskT : ArtifactProducer<ValueT> {
    // register output
    val outputBuilder = when (val output = task.outputArtifact) {
        is DirectoryProperty -> task.outputs.dir(output)
        is RegularFileProperty -> task.outputs.file(output)
        else -> throw RuntimeException("Unexpected output type: ${output.javaClass}")
    }
    outputBuilder.withPropertyName("outputArtifact")
}


internal fun <TaskT, ValueT> processOutput(task: TaskT, location: Provider<ValueT>? = null) where TaskT : DefaultTask, TaskT : ArtifactProducer<ValueT> {
    // set output location
    if (location != null) {
        task.outputArtifact.set(location)
    }

    registerOutput(task)
}
