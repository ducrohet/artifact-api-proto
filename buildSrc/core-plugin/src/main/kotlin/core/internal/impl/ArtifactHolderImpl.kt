package core.internal.impl

import core.api.*
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.RegularFile
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
            TODO("")
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
            TODO("")
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
            TODO("")
        }
        else -> {
            throw RuntimeException("unsupported MultiArtifactType")
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
            outputProvider: (TaskT) -> Property<ValueT>
    ) {
        when (artifactType) {
            is MultiFileArtifactType -> {
                multiFile.produces(artifactType, taskProvider, outputProvider  as ((TaskT) -> Property<RegularFile>))
            }
            is MultiDirectoryArtifactType -> {
                TODO("")
            }
            else -> {
                throw RuntimeException("unsupported MultiArtifactType")
            }
        }
    }

    override fun <TaskT> transform(
            artifactType: SingleDirectoryArtifactType,
            taskName: String,
            taskClass: Class<TaskT>,
            configAction: (TaskT) -> Unit
    ): TaskProvider<TaskT> where TaskT : DefaultTask, TaskT : DirectoryConsumerTask, TaskT : DirectoryProducerTask {
        return singleDir.transform(
                artifactType,
                taskName,
                taskClass,
                configAction)

    }

    override fun <TaskT> transform(
            artifactType: SingleFileArtifactType,
            taskName: String,
            taskClass: Class<TaskT>,
            configAction: (TaskT) -> Unit
    ): TaskProvider<TaskT> where TaskT : DefaultTask, TaskT : FileConsumerTask, TaskT : FileProducerTask {
        return singleFile.transform(
                artifactType,
                taskName,
                taskClass,
                configAction)
    }

    override fun <TaskT> transform(
            artifactType: MultiFileArtifactType,
            taskName: String, taskClass: Class<TaskT>,
            configAction: (TaskT) -> Unit
    ): TaskProvider<TaskT> where TaskT: DefaultTask, TaskT : FileListConsumerTask, TaskT : FileProducerTask {
        return multiFile.transform(
                artifactType,
                taskName,
                taskClass,
                configAction)
    }

    override fun <TaskT> append(
            artifactType: MultiDirectoryArtifactType,
            taskName: String, taskClass: Class<TaskT>,
            configAction: (TaskT) -> Unit
    ): TaskProvider<TaskT> where TaskT: DefaultTask, TaskT : DirectoryProducerTask {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <TaskT> append(
            artifactType: MultiFileArtifactType,
            taskName: String,
            taskClass: Class<TaskT>,
            configAction: (TaskT) -> Unit
    ): TaskProvider<TaskT> where TaskT: DefaultTask, TaskT : FileProducerTask {
        return multiFile.append(
                artifactType,
                taskName,
                taskClass,
                configAction)
    }

    fun finalizeLocations() {
        singleFile.finalizeLocations()
        singleDir.finalizeLocations()
    }
}