package core.internal.impl

import core.api.*
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.HasMultipleValues
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskInputFilePropertyBuilder
import org.gradle.api.tasks.TaskProvider


@Suppress("UnstableApiUsage")
class ArtifactHandlerImpl<TaskT: DefaultTask>(
        private val artifactHolder: ArtifactHolderImpl,
        private val taskProvider: TaskProvider<TaskT>
): ArtifactHandler<TaskT> {

    override fun <ValueT : FileSystemLocation, ProviderT : Provider<ValueT>> input(
            artifactType: SingleArtifactType<ValueT, ProviderT>,
            inputProvider: (TaskT) -> Property<ValueT>): ArtifactHandler<TaskT> {
        val artifact = artifactHolder.getArtifact(artifactType)

        taskProvider.configure {
            inputProvider(it).run {
                set(artifact)
                disallowChanges()
            }
        }

        return this
    }

    override fun <ValueT : FileSystemLocation, ProviderT : Provider<out Iterable<ValueT>>> input(
            artifactType: MultiArtifactType<ValueT, ProviderT>,
            inputProvider: (TaskT) -> HasMultipleValues<ValueT>): ArtifactHandler<TaskT> {
        val artifact = artifactHolder.getArtifact(artifactType)

        taskProvider.configure {
            inputProvider(it).run {
                set(artifact)
                disallowChanges()
            }
        }

        return this
    }
}