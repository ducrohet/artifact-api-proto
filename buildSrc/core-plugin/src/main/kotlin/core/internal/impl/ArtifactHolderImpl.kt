package core.internal.impl

import core.api.*
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.*
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

/**
 * Implementation of [ArtifactHolder].
 *
 * Mostly delegates to [SingleDirectoryHolder], [SingleFileHolder], and [MultiFileHolder]
 *
 * (MultiDirectoryHolder TBD)
 */
@Suppress("UnstableApiUsage", "UNCHECKED_CAST")
class ArtifactHolderImpl(project: Project) : ArtifactHolder {
    private val singleDir = SingleDirectoryHolder(project)
    private val singleFile = SingleFileHolder(project)
    private val multiFile = MultiFileHolder(project)
    private val multiDir = MultiDirectoryHolder(project)
    private val multiMixed = MultiMixedHolder(project)

    override fun <ValueT: FileSystemLocation, ProviderT: Provider<ValueT>> getArtifact(
            artifactType : SingleArtifactType<ValueT, ProviderT>
    ) : ProviderT = when (artifactType) {
        is SingleDirectoryArtifactType -> {
            singleDir.getArtifact(artifactType) as ProviderT
        }
        is SingleFileArtifactType -> {
            singleFile.getArtifact(artifactType) as ProviderT
        }
        else -> {
            throw RuntimeException("unsupported SingleArtifactType")
        }
    }

    override fun <ValueT: FileSystemLocation, ProviderT: Provider<out Iterable<ValueT>>> getArtifact(
            artifactType : MultiArtifactType<ValueT, ProviderT>
    ) : ProviderT = when (artifactType) {
        is MultiFileArtifactType -> {
            multiFile.getArtifact(artifactType) as ProviderT
        }
        is MultiDirectoryArtifactType -> {
            multiDir.getArtifact(artifactType) as ProviderT
        }
        is MultiMixedArtifactType -> {
            multiMixed.getArtifact(artifactType) as ProviderT
        }
        else -> {
            throw RuntimeException("unsupported MultiArtifactType")
        }
    }

    internal fun <ValueT: FileSystemLocation, ProviderT: Provider<ValueT>> hasTransforms(
            artifactType : SingleArtifactType<ValueT, ProviderT>
    ) : Boolean = when (artifactType) {
        is SingleDirectoryArtifactType -> {
            singleDir.hasTransforms(artifactType)
        }
        is SingleFileArtifactType -> {
            singleFile.hasTransforms(artifactType)
        }
        else -> {
            throw RuntimeException("unsupported SingleArtifactType")
        }
    }

    internal fun <ValueT: FileSystemLocation, ProviderT: Provider<out Iterable<ValueT>>> hasTransforms(
            artifactType : MultiArtifactType<ValueT, ProviderT>
    ): Boolean = when (artifactType) {
        is MultiFileArtifactType -> {
            multiFile.hasTransforms(artifactType)
        }
        is MultiDirectoryArtifactType -> {
            multiDir.hasTransforms(artifactType)
        }
        else -> {
            throw RuntimeException("unsupported MultiArtifactType")
        }
    }

    internal fun <ValueT: FileSystemLocation, ProviderT: Provider<out Iterable<ValueT>>> hasAppend(
            artifactType : MultiArtifactType<ValueT, ProviderT>
    ): Boolean = when (artifactType) {
        is MultiFileArtifactType -> {
            multiFile.hasAppend(artifactType)
        }
        is MultiDirectoryArtifactType -> {
            multiDir.hasAppend(artifactType)
        }
        else -> {
            throw RuntimeException("unsupported MultiArtifactType")
        }
    }

    internal fun <ValueT: FileSystemLocation, ProviderT: Provider<ValueT>> hasProducer(
            artifactType : SingleArtifactType<ValueT, ProviderT>
    ) : Boolean = when (artifactType) {
        is SingleDirectoryArtifactType -> {
            singleDir.hasProducer(artifactType)
        }
        is SingleFileArtifactType -> {
            singleFile.hasProducer(artifactType)
        }
        else -> {
            throw RuntimeException("unsupported SingleArtifactType")
        }
    }


    /**
     * Register the original producer of the given artifact.
     *
     * @param artifactType the artifact to update
     * @param taskProvider the task that generates the first version of the artifact
     * @param outputProvider a provider that returns the [Property] that contains the output
     */
    internal fun <ValueT: FileSystemLocation, ProviderT: Provider<ValueT>, TaskT: DefaultTask> produces(
            artifactType : SingleArtifactType<ValueT, ProviderT>,
            taskProvider: TaskProvider<TaskT>,
            outputProvider: (TaskT) -> Property<ValueT>
    ) {
        when (artifactType) {
            is SingleDirectoryArtifactType -> {
                singleDir.produces(artifactType, taskProvider, outputProvider as ((TaskT) -> Property<Directory>))
            }
            is SingleFileArtifactType -> {
                singleFile.produces(artifactType, taskProvider, outputProvider  as ((TaskT) -> Property<RegularFile>))
            }
            else -> {
                throw RuntimeException("unsupported SingleArtifactType")
            }
        }
    }

    /**
     * Register the original producer of the given artifact.
     *
     * @param artifactType the artifact to update
     * @param taskProvider the task that generates the first version of the artifact
     * @param outputProvider a provider that returns the [Property] that contains the output
     */
    internal fun <ValueT: FileSystemLocation, ProviderT: Provider<out Iterable<ValueT>>, TaskT: DefaultTask> produces(
            artifactType : MultiArtifactType<ValueT, ProviderT>,
            taskProvider: TaskProvider<TaskT>,
            outputProvider: (TaskT) -> Property<out ValueT>
    ) {
        when (artifactType) {
            is MultiFileArtifactType -> {
                multiFile.produces(artifactType, taskProvider, outputProvider  as ((TaskT) -> Property<RegularFile>))
            }
            is MultiDirectoryArtifactType -> {
                multiDir.produces(artifactType, taskProvider, outputProvider  as ((TaskT) -> Property<Directory>))
            }
            is MultiMixedArtifactType -> {
                multiMixed.produces(artifactType, taskProvider, outputProvider as ((TaskT) -> Property<FileSystemLocation>))
            }
            else -> {
                throw RuntimeException("unsupported MultiArtifactType")
            }
        }
    }

    override fun <TaskT> replace(
            artifactType: SingleFileArtifactType,
            taskName: String,
            taskClass: Class<TaskT>
    ): ArtifactHandler<TaskT> where TaskT: DefaultTask, TaskT : FileProducerTask {
        return singleFile.replace(this, artifactType, taskName, taskClass)
    }

    override fun <TaskT: DefaultTask> transform(
            artifactType: SingleDirectoryArtifactType,
            taskProvider: TaskProvider<TaskT>,
            inputProvider: (TaskT) -> DirectoryProperty,
            outputProvider: (TaskT) -> DirectoryProperty
    ) {
        singleDir.transform(
                artifactType,
                taskProvider,
                inputProvider,
                outputProvider)
    }

    override fun <TaskT: DefaultTask> transform(
            artifactType: SingleFileArtifactType,
            taskProvider: TaskProvider<TaskT>,
            inputProvider: (TaskT) -> RegularFileProperty,
            outputProvider: (TaskT) -> RegularFileProperty
    ) {
        singleFile.transform(
                artifactType,
                taskProvider,
                inputProvider,
                outputProvider)
    }

    override fun <TaskT : DefaultTask> transform(
            artifactType: MultiFileArtifactType,
            taskProvider: TaskProvider<TaskT>,
            inputProvider: (TaskT) -> ListProperty<RegularFile>,
            outputProvider: (TaskT) -> RegularFileProperty) {
        multiFile.transform(
                artifactType,
                taskProvider,
                inputProvider,
                outputProvider)
    }

    override fun <TaskT : DefaultTask> transform(
            artifactType: MultiDirectoryArtifactType,
            taskProvider: TaskProvider<TaskT>,
            inputProvider: (TaskT) -> ListProperty<Directory>,
            outputProvider: (TaskT) -> DirectoryProperty) {
        multiDir.transform(
                artifactType,
                taskProvider,
                inputProvider,
                outputProvider)
    }

    override fun <TaskT: DefaultTask> append(
            artifactType: MultiDirectoryArtifactType,
            taskProvider: TaskProvider<TaskT>,
            outputProvider: (TaskT) -> DirectoryProperty
    ) {
        return multiDir.append(
                artifactType,
                taskProvider,
                outputProvider)
    }

    override fun <TaskT: DefaultTask> append(
            artifactType: MultiFileArtifactType,
            taskProvider: TaskProvider<TaskT>,
            outputProvider: (TaskT) -> RegularFileProperty
    ) {
        return multiFile.append(
                artifactType,
                taskProvider,
                outputProvider)
    }

    override fun <TaskT : DefaultTask> append(
            artifactType: MultiMixedArtifactType,
            taskProvider: TaskProvider<TaskT>,
            outputProvider: (TaskT) -> Property<out FileSystemLocation>
    ) {
        return multiMixed.append(
                artifactType,
                taskProvider,
                outputProvider)
    }

    fun finalizeLocations() {
        singleFile.finalizeLocations()
        singleDir.finalizeLocations()
    }
}