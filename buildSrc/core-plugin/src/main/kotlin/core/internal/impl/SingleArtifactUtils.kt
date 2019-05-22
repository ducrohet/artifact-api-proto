@file:Suppress("UnstableApiUsage")
package core.internal.impl

import core.api.*
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.*
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskInputFilePropertyBuilder
import org.gradle.api.tasks.TaskProvider
import java.io.File
import java.util.*

class SingleDirectoryHolder(project: Project): SingleArtifactHolder<SingleDirectoryArtifactType, Directory, Provider<Directory>>(project) {

    override val map = EnumMap<SingleDirectoryArtifactType, SingleArtifactInfo<Directory>>(SingleDirectoryArtifactType::class.java)

    override fun newProperty(): Property<Directory> = project.objects.directoryProperty()
    override fun newLocation() = File(project.buildDir, "foo2")

    init {
        for (artifact in SingleDirectoryArtifactType.values()) {
            init(artifact)
        }
    }
}

class SingleFileHolder(project: Project): SingleArtifactHolder<SingleFileArtifactType, RegularFile, Provider<RegularFile>>(project) {

    override val map = EnumMap<SingleFileArtifactType, SingleArtifactInfo<RegularFile>>(SingleFileArtifactType::class.java)

    override fun newProperty(): Property<RegularFile> = project.objects.fileProperty()
    override fun newLocation() = File(project.buildDir, "foo2.txt")

    init {
        for (artifact in SingleFileArtifactType.values()) {
            init(artifact)
        }
    }
}

class SingleArtifactInfo<ValueT>(
        val finalArtifact: Property<ValueT>,
        currentArtifactProperty: Property<ValueT>,
        private val firstArtifact: Property<ValueT>
) {
    var isInitialized: Boolean = false
        private set

    var currentArtifact: Provider<ValueT>

    fun setFirstArtifact(artifact: Provider<ValueT>) {
        firstArtifact.set(artifact)
        isInitialized = true
    }

    init {
        currentArtifactProperty.set(firstArtifact)
        currentArtifact = currentArtifactProperty
        finalArtifact.set(currentArtifact)
    }
}

abstract class SingleArtifactHolder<ArtifactT: SingleArtifactType<ValueT, ProviderT>, ValueT: FileSystemLocation, ProviderT : Provider<ValueT>>(
        protected val project: Project
) {

    protected abstract val map: MutableMap<ArtifactT, SingleArtifactInfo<ValueT>>

    protected abstract fun newProperty(): Property<ValueT>

    protected fun init(artifactType: ArtifactT) {
        map[artifactType] = SingleArtifactInfo(newProperty(), newProperty(), newProperty())
    }

    fun getArtifact(artifactType : ArtifactT) : Provider<ValueT> =
            map[artifactType]?.finalArtifact ?: throw RuntimeException("Did not find artifact for $artifactType")

    fun produces(artifactType : ArtifactT, artifact: Provider<ValueT>) {
        val info = map[artifactType] ?: throw RuntimeException("Did not find artifact for $artifactType")
        if (info.isInitialized) {
            throw RuntimeException("Artifact $artifactType already initialized")
        }

        info.setFirstArtifact(artifact)
    }

    fun <TaskT> transform(
            artifactType : ArtifactT,
            taskName: String,
            taskClass: Class<TaskT>,
            configAction: (TaskT) -> Unit) : TaskProvider<TaskT> where TaskT: DefaultTask, TaskT: ArtifactConsumer<ValueT>, TaskT: ArtifactProducer<ValueT> {
        val info = map[artifactType] ?: throw RuntimeException("Did not find artifact for $artifactType")

        val previousCurrent = info.currentArtifact

        val newTask = project.tasks.register(taskName, taskClass)

        // get provider for the newer version of the artifact
        val newCurrent = newTask.flatMap { it.outputArtifact }

        // update final and current
        info.finalArtifact.set(newCurrent)
        info.currentArtifact = newCurrent

        newTask.configure {
            // set input value and register input with sensitivity
            it.inputArtifact.set(previousCurrent)
            val inputBuilder: TaskInputFilePropertyBuilder = when (val input = it.inputArtifact) {
                is DirectoryProperty -> {
                    it.inputs.dir(input)
                }
                is RegularFileProperty -> {
                    it.inputs.file(input)
                }
                else -> {
                    throw RuntimeException("Unexpected input type: ${input.javaClass}")
                }
            }

            inputBuilder.withPropertyName("inputArtifact").withPathSensitivity(artifactType.sensitivity)
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

    protected abstract fun newLocation(): File
}
