@file:Suppress("UNCHECKED_CAST", "UnstableApiUsage")
package core.internal.impl

import core.api.*
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Transformer
import org.gradle.api.file.*
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
    override fun setIntermediateLocation(property: Property<out RegularFile>, artifactType: MultiFileArtifactType, taskName: String) {
        val output = property as RegularFileProperty
        output.set(File(project.buildDir, "intermediates/$taskName/$artifactType.txt"))
    }

    init {
        for (artifact in MultiFileArtifactType.values()) {
            init(artifact)
        }
    }
}

class MultiDirectoryHolder(
        project: Project
): MultiArtifactHolder<MultiDirectoryArtifactType, Directory, Provider<out Iterable<Directory>>>(project) {

    override val map = EnumMap<MultiDirectoryArtifactType, MultiArtifactInfo<Directory>>(MultiDirectoryArtifactType::class.java)

    override fun newListProperty(): ListProperty<Directory> = project.objects.listProperty(Directory::class.java)
    override fun setIntermediateLocation(property: Property<out Directory>, artifactType: MultiDirectoryArtifactType, taskName: String) {
        val output = property as DirectoryProperty
        output.set(File(project.buildDir, "intermediates/$taskName/$artifactType"))
    }

    init {
        for (artifact in MultiDirectoryArtifactType.values()) {
            init(artifact)
        }
    }
}

class MultiMixedHolder(
        project: Project
): MultiArtifactHolder<MultiMixedArtifactType, FileSystemLocation, Provider<out Iterable<FileSystemLocation>>>(project) {

    override val map = EnumMap<MultiMixedArtifactType, MultiArtifactInfo<FileSystemLocation>>(MultiMixedArtifactType::class.java)

    override fun newListProperty(): ListProperty<FileSystemLocation> = project.objects.listProperty(FileSystemLocation::class.java)
    override fun setIntermediateLocation(
            property: Property<out FileSystemLocation>,
            artifactType: MultiMixedArtifactType,
            taskName: String) {
        when (property) {
            is DirectoryProperty -> property.set(File(project.buildDir, "intermediates/$taskName/$artifactType"))
            is RegularFileProperty -> property.set(File(project.buildDir, "intermediates/$taskName/$artifactType.txt"))
            else -> throw RuntimeException("Unsupported Property")
        }
    }

    init {
        for (artifact in MultiMixedArtifactType.values()) {
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
    /**
     * Final Artifact. Always the final value of the artifact. This is dynamic and is updated as new transforms
     * are added.
     */
    val finalArtifact: ListProperty<ValueT> = propertyProvider()

    /**
     * The current artifact version. Every new transform updates this to the output of the new transform
     *
     * @see [MultiArtifactInfo.setNewOutput]
     */
    private var currentArtifact: Provider<out Iterable<ValueT>>

    var hasAppend: Boolean = false
        private set
    var hasTransforms: Boolean = false
        private set

    /**
     * Wrapper around the first provider. This is used to handle transforms while the task that produce the first
     * version of the artifact has not yet been set.
     *
     * This also receives all the appended outputs.
     */
    private val firstArtifact: ListProperty<ValueT> = propertyProvider()

    /**
     * Sets the value of the first artifact.
     */
    fun setFirstProvider(artifact: Provider<ValueT>) {
        firstArtifact.add(artifact)
    }

    /**
     * Appends a new output to the artifact.
     */
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
    protected abstract fun setIntermediateLocation(property: Property<out ValueT>, artifactType: ArtifactT, taskName: String)

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
            setIntermediateLocation(outputProvider(it), artifactType, it.name)
        }
    }

    fun <TaskT: DefaultTask> transform(
            artifactType : ArtifactT,
            taskProvider: TaskProvider<TaskT>,
            inputProvider: (TaskT) -> ListProperty<ValueT>,
            outputProvider: (TaskT) -> Property<ValueT>
    ) {
        val info = map[artifactType] ?: throw RuntimeException("Did not find artifact for $artifactType")

        // update the info with the new output and get the previous output. This will be used
        // to configure the input of the task
        val previousCurrent = info.setNewOutput(
                newListProperty().also { w -> w.add(taskProvider.flatMap { outputProvider(it) })})

        taskProvider.configure {
            inputProvider(it).run {
                set(previousCurrent)
                disallowChanges()
            }

            outputProvider(it).run {
                setIntermediateLocation(this, artifactType, it.name)
                disallowChanges()
            }
        }
    }

    open fun <TaskT: DefaultTask> append(
            artifactType : ArtifactT,
            taskProvider: TaskProvider<TaskT>,
            outputProvider: (TaskT) -> Property<out ValueT>
    ) {
        val info = map[artifactType] ?: throw RuntimeException("Did not find artifact for $artifactType")

        // append the task output
        info.append(taskProvider.flatMap { outputProvider(it) })

        taskProvider.configure {
            outputProvider(it).run {
                setIntermediateLocation(this, artifactType, it.name)
                disallowChanges()
            }
        }
    }
}
