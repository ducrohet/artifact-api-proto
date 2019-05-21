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

@Suppress("UnstableApiUsage")
class SingleDirectoryHolder(project: Project): SingleArtifactHolder<SingleDirectoryArtifactType, Directory, Provider<Directory>>(project) {

    override val wrapperMap = EnumMap<SingleDirectoryArtifactType, Property<Directory>>(SingleDirectoryArtifactType::class.java)
    override val latestMap = EnumMap<SingleDirectoryArtifactType, Provider<Directory>>(SingleDirectoryArtifactType::class.java)

    override fun newWrapper(): Property<Directory> = project.objects.directoryProperty()
    override fun newLocation() = File(project.buildDir, "foo2")
}

@Suppress("UnstableApiUsage")
class SingleFileHolder(project: Project): SingleArtifactHolder<SingleFileArtifactType, RegularFile, Provider<RegularFile>>(project) {

    override val wrapperMap = EnumMap<SingleFileArtifactType, Property<RegularFile>>(SingleFileArtifactType::class.java)
    override val latestMap = EnumMap<SingleFileArtifactType, Provider<RegularFile>>(SingleFileArtifactType::class.java)

    override fun newWrapper(): Property<RegularFile> = project.objects.fileProperty()
    override fun newLocation() = File(project.buildDir, "foo2.txt")
}

@Suppress("UnstableApiUsage")
abstract class SingleArtifactHolder<ArtifactT: SingleArtifactType<ValueT, ProviderT>, ValueT, ProviderT : Provider<ValueT>>(protected val project: Project) {

    protected abstract val wrapperMap: MutableMap<ArtifactT, Property<ValueT>>
    protected abstract val latestMap: MutableMap<ArtifactT, Provider<ValueT>>

    protected abstract fun newWrapper(): Property<ValueT>

    fun getArtifact(artifactType : ArtifactT) : Provider<ValueT> =
            wrapperMap[artifactType] ?: throw RuntimeException("Did not find artifact for $artifactType")

    fun produces(artifactType : ArtifactT, artifact: Provider<ValueT>) {
        if (wrapperMap.containsKey(artifactType)) {
            throw RuntimeException("singleDirMap already contains $artifactType")
        }

        val wrapper = newWrapper()
        wrapper.set(artifact)
        wrapperMap[artifactType] = wrapper
        latestMap[artifactType] = artifact
    }

    fun <TaskT> transform(
            artifactType : ArtifactT,
            taskName: String,
            taskClass: Class<TaskT>,
            configAction: (TaskT) -> Unit) : TaskProvider<TaskT> where TaskT: DefaultTask, TaskT: ArtifactConsumer<ValueT>, TaskT: ArtifactProducer<ValueT> {

        val newTask = project.tasks.register(taskName, taskClass)

        val wrapper = wrapperMap[artifactType] ?: throw RuntimeException("Did not find artifact for $artifactType")
        val previousLatest = latestMap[artifactType]?: throw RuntimeException("Did not find artifact for $artifactType")

        // get provider for the newer version of the artifact
        val newLatest = newTask.flatMap { it.outputArtifact }

        // update wrapper and latest
        wrapper.set(newLatest)
        latestMap[artifactType] = newLatest

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
