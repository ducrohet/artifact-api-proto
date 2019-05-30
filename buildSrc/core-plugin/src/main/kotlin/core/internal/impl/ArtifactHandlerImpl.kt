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

    override fun finish(configAction: (TaskT) -> Unit): TaskProvider<TaskT> {
        taskProvider.configure(configAction)
        return taskProvider
    }

    override fun <ValueT : FileSystemLocation, ProviderT : Provider<ValueT>> input(
            artifactType: SingleArtifactType<ValueT, ProviderT>,
            inputProvider: (TaskT) -> Property<ValueT>): ArtifactHandler<TaskT> {
        val artifact = artifactHolder.getArtifact(artifactType)

        taskProvider.configure {
            val inputProperty = inputProvider(it)
            inputProperty.set(artifact)

            // register the inputs on the task, including setting the sensitivity and normalizer
            val inputBuilder: TaskInputFilePropertyBuilder = when (inputProperty) {
                is DirectoryProperty -> {
                    it.inputs.dir(inputProperty)
                }
                is RegularFileProperty -> {
                    it.inputs.file(inputProperty)
                }
                else -> {
                    throw RuntimeException("Unexpected input type: ${inputProperty.javaClass}")
                }
            }

         //   inputBuilder.withPropertyName("inputArtifact")
            if (artifactType.sensitivity != null) {
                @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                inputBuilder.withPathSensitivity(artifactType.sensitivity)
            } else if (artifactType.normalizer != null) {
                @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                inputBuilder.withNormalizer(artifactType.normalizer)
            }
        }

        return this
    }

    override fun <ValueT : FileSystemLocation, ProviderT : Provider<out Iterable<ValueT>>> input(
            artifactType: MultiArtifactType<ValueT, ProviderT>,
            inputProvider: (TaskT) -> HasMultipleValues<ValueT>): ArtifactHandler<TaskT> {
        val artifact = artifactHolder.getArtifact(artifactType)

        taskProvider.configure {
            val inputProperty: HasMultipleValues<ValueT> = inputProvider(it)
            inputProperty.set(artifact)

            // register the inputs on the task, including setting the sensitivity and normalizer
            val inputBuilder: TaskInputFilePropertyBuilder = it.inputs.files(inputProperty)
       //     inputBuilder.withPropertyName("inputArtifact")
            if (artifactType.sensitivity != null) {
                @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                inputBuilder.withPathSensitivity(artifactType.sensitivity)
            } else if (artifactType.normalizer != null) {
                @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                inputBuilder.withNormalizer(artifactType.normalizer)
            }
        }

        return this
    }
}