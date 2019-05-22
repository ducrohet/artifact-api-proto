package core.internal.impl

import core.api.*
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.RegularFile
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
    ) : Provider<ValueT> = when (artifactType) {
        is SingleDirectoryArtifactType -> {
            singleDir.getArtifact(artifactType) as Provider<ValueT>
        }
        is SingleFileArtifactType -> {
            singleFile.getArtifact(artifactType) as Provider<ValueT>
        }
        else -> {
            throw RuntimeException("unsupported SingleArtifactType")
        }
    }

    override fun <ValueT: FileSystemLocation, ProviderT: Provider<out Iterable<ValueT>>> getArtifact(
            artifactType : MultiArtifactType<ValueT, ProviderT>
    ) : Provider<out Iterable<ValueT>> = when (artifactType) {
        is MultiFileArtifactType -> {
            multiFile.getArtifact(artifactType) as Provider<out Iterable<ValueT>>
        }
        is MultiDirectoryArtifactType -> {
            TODO("")
        }
        else -> {
            throw RuntimeException("unsupported MultiArtifactType")
        }
    }

    internal fun <ValueT: FileSystemLocation, ProviderT: Provider<ValueT>> produces(
            artifactType : SingleArtifactType<ValueT, ProviderT>,
            artifact: Provider<ValueT>
    ) {
        when (artifactType) {
            is SingleDirectoryArtifactType -> {
                singleDir.produces(artifactType, artifact as Provider<Directory>)
            }
            is SingleFileArtifactType -> {
                singleFile.produces(artifactType, artifact as Provider<RegularFile>)
            }
            else -> {
                throw RuntimeException("unsupported SingleArtifactType")
            }
        }
    }

    internal fun <ValueT: FileSystemLocation, ProviderT: Provider<out Iterable<ValueT>>> produces(
            artifactType : MultiArtifactType<ValueT, ProviderT>,
            artifact: Provider<ValueT>
    ) {
        when (artifactType) {
            is MultiFileArtifactType -> {
                multiFile.produces(artifactType, artifact as Provider<RegularFile>)
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
}