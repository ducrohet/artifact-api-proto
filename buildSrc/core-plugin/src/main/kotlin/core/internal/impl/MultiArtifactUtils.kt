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
import java.util.*

class MultiFileHolder(
        project: Project
): MultiArtifactHolder<MultiFileArtifactType, RegularFile, Provider<out Iterable<RegularFile>>>(project) {

    override val map = EnumMap<MultiFileArtifactType, MultiArtifactInfo<RegularFile>>(MultiFileArtifactType::class.java)

    override fun newListProperty(): ListProperty<RegularFile> = project.objects.listProperty(RegularFile::class.java)
    override fun newIntermediateLocation(artifactType: MultiFileArtifactType, taskName: String): File =
            File(project.buildDir, "intermediates/$taskName/$artifactType.txt")

    init {
        for (artifact in MultiFileArtifactType.values()) {
            init(artifact)
        }
    }
}

/**
 * class that holds the info for a given artifact.
 *
 * It contains references to the current last version of the artifact, a dynamic final artifact property, etc..
 */
class MultiArtifactInfo<ValueT: FileSystemLocation>(
        propertyProvider: () -> ListProperty<ValueT>
) {
    val finalArtifact: ListProperty<ValueT> = propertyProvider()

    private var currentArtifact: Provider<out Iterable<ValueT>>

    var hasAppend: Boolean = false
        private set
    var hasTransforms: Boolean = false
        private set

    private val firstArtifact: ListProperty<ValueT> = propertyProvider()

    fun setFirstProvider(artifact: Provider<ValueT>) {
        firstArtifact.add(artifact)
    }

    fun append(artifact: Provider<ValueT>) {
        firstArtifact.add(artifact)
        hasAppend = true
    }

    /**
     * Sets a new output and return the old one
     */
    fun setNewOutput(artifact: Provider<out Iterable<ValueT>>): Provider<out Iterable<ValueT>> {
        hasTransforms = true

        val oldCurrent = currentArtifact

        // update final and current
        finalArtifact.set(artifact)
        currentArtifact = artifact

        return oldCurrent
    }

    init {
        val currentArtifactProperty = propertyProvider()
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
    protected abstract fun newIntermediateLocation(artifactType: ArtifactT, taskName: String): File

    protected fun init(artifactType: ArtifactT) {
        map[artifactType] = MultiArtifactInfo(::newListProperty)
    }

    fun getArtifact(artifactType : ArtifactT) : Provider<out Iterable<ValueT>> =
            map[artifactType]?.finalArtifact ?: throw RuntimeException("Did not find artifact for $artifactType")

    fun hasAppend(artifactType: ArtifactT): Boolean {
        val info = map[artifactType] ?: throw RuntimeException("Did not find artifact for $artifactType")

        return info.hasAppend
    }

    fun hasTransforms(artifactType: ArtifactT): Boolean {
        val info = map[artifactType] ?: throw RuntimeException("Did not find artifact for $artifactType")

        return info.hasTransforms
    }

    fun <TaskT: DefaultTask> produces(
            artifactType: ArtifactT,
            taskProvider: TaskProvider<TaskT>,
            outputProvider: (TaskT) -> Property<ValueT>
    ) {
        val info = map[artifactType] ?: throw RuntimeException("Did not find artifact for $artifactType")

        info.setFirstProvider(taskProvider.flatMap { outputProvider(it) })

        taskProvider.configure {
            when (val output = outputProvider(it)) {
                is DirectoryProperty -> output.set(newIntermediateLocation(artifactType, it.name))
                is RegularFileProperty -> output.set(newIntermediateLocation(artifactType, it.name))
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

        val newTask = project.tasks.register(taskName, taskClass)

        // create a new property?
        val previousCurrent = info.setNewOutput(
                newListProperty().also { w -> w.add(newTask.flatMap { it.outputArtifact })})

        newTask.configure {
            // set input and register input with sensitivity
            it.inputArtifacts.set(previousCurrent)
            val inputBuilder = it.inputs
                    .files(it.inputArtifacts)
                    .withPropertyName("inputArtifacts")
            if (artifactType.sensitivity != null) {
                @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                inputBuilder.withPathSensitivity(artifactType.sensitivity)
            } else if (artifactType.normalizer != null) {
                @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                inputBuilder.withNormalizer(artifactType.normalizer)
            }

            // set output location and register output
            processOutput(it, newIntermediateLocation(artifactType, it.name))

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
            processOutput(it, newIntermediateLocation(artifactType, it.name))
            // run the user's configuration action
            configAction.invoke(it)
        }

        return newTask
    }
}
