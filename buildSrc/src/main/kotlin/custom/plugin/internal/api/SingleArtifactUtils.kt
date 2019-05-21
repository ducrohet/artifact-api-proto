@file:Suppress("UnstableApiUsage")
package custom.plugin.internal.api

import custom.plugin.api.*
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
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
        val finalArtifact: Property<ValueT>
) {
    lateinit var currentArtifact: Provider<ValueT>
    val isInitialized: Boolean
        get() = ::currentArtifact.isInitialized
}

abstract class SingleArtifactHolder<ArtifactT: SingleArtifactType<ValueT, ProviderT>, ValueT, ProviderT : Provider<ValueT>>(protected val project: Project) {

    protected abstract val map: MutableMap<ArtifactT, SingleArtifactInfo<ValueT>>

    protected abstract fun newProperty(): Property<ValueT>

    protected fun init(artifactType: ArtifactT) {
        map[artifactType] = SingleArtifactInfo(newProperty())
    }

    fun getArtifact(artifactType : ArtifactT) : Provider<ValueT> =
            map[artifactType]?.finalArtifact ?: throw RuntimeException("Did not find artifact for $artifactType")

    fun produces(artifactType : ArtifactT, artifact: Provider<ValueT>) {
        val info = map[artifactType] ?: throw RuntimeException("Did not find artifact for $artifactType")
        if (info.isInitialized) {
            throw RuntimeException("Artifact $artifactType already initialized")
        }

        info.finalArtifact.set(artifact)
        info.currentArtifact = artifact
    }

    fun <TaskT> transform(
            artifactType : ArtifactT,
            taskName: String,
            taskClass: Class<TaskT>,
            configAction: (TaskT) -> Unit) : TaskProvider<TaskT> where TaskT: DefaultTask, TaskT: ArtifactConsumer<ValueT>, TaskT: ArtifactProducer<ValueT> {
        val info = map[artifactType] ?: throw RuntimeException("Did not find artifact for $artifactType")
        if (!info.isInitialized) {
            throw RuntimeException("Artifact $artifactType was not initialized")
        }

        val newTask = project.tasks.register(taskName, taskClass)

        val previousLatest = info.currentArtifact

        // get provider for the newer version of the artifact
        val newLatest = newTask.flatMap { it.outputArtifact }

        // update final and current
        info.finalArtifact.set(newLatest)
        info.currentArtifact = newLatest

        newTask.configure {
            // set input value and register input with sensitivity
            it.inputArtifact.set(previousLatest)
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
