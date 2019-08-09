@file:Suppress("UNCHECKED_CAST", "UnstableApiUsage")

package core.api

import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.provider.HasMultipleValues
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskProvider

/**
 * Base API for manipulating artifacts.
 *
 * Contains methods to read, transform and append to artifacts.
 */
interface ArtifactHolder {
    /**
     * Returns an artifact as a [Provider] of [RegularFile] or [Directory]
     * This is compatible with [RegularFileProperty] and [DirectoryProperty]
     *
     * This is always the final version of this artifact. This is to be used for tasks wanting to read only an artifact,
     * not transform it.
     *
     * @param artifactType the type of the artifact
     * @return the artifact as a provider containing both value and task dependency information
     */
    fun <ValueT: FileSystemLocation, ProviderT: Provider<ValueT>> getArtifact(
            artifactType : SingleArtifactType<ValueT, ProviderT>
    ) : ProviderT

    /**
     * Returns an artifact as a [Provider] of iterable [RegularFile] or [Directory]
     * This is compatible with [ListProperty]
     *
     * This is always the final version of this artifact. This is to be used for tasks wanting to read only an artifact,
     * not transform it.
     *
     * @param artifactType the type of the artifact
     * @return the artifact as a provider containing both value and task dependency information
     */
    fun <ValueT: FileSystemLocation, ProviderT: Provider<out Iterable<ValueT>>> getArtifact(
            artifactType : MultiArtifactType<ValueT, ProviderT>
    ) : ProviderT

    /**
     * Replaces an artifact.
     *
     * This registers the task as the original creator of this artifact (which can then be transformed by others).
     * The original task that normally generates this artifact is not created.
     *
     * @param artifactType the type of the artifact to transform
     * @param taskProvider the provider for the task doing the replace
     * @param outputProvider the lambda returning the output field of the task
     *
     */
    fun <TaskT: DefaultTask> replace(
            artifactType: SingleFileArtifactType,
            taskProvider: TaskProvider<TaskT>,
            outputProvider: (TaskT) -> RegularFileProperty
    )

    /**
     * Transforms an artifact.
     *
     * This configures the task input and output.
     *
     * @param artifactType the type of the artifact to transform
     * @param taskProvider the provider for the task doing the transform
     * @param inputProvider the lambda returning the input field of the task
     * @param outputProvider the lambda returning the output field of the task
     *
     */
    fun <TaskT: DefaultTask> transform(
            artifactType: SingleDirectoryArtifactType,
            taskProvider: TaskProvider<TaskT>,
            inputProvider: (TaskT) -> DirectoryProperty,
            outputProvider: (TaskT) -> DirectoryProperty
    )

    /**
     * Transforms an artifact.
     *
     * This configures the task input and output.
     *
     * @param artifactType the type of the artifact to transform
     * @param taskProvider the provider for the task doing the transform
     * @param inputProvider the lambda returning the input field of the task
     * @param outputProvider the lambda returning the output field of the task
     *
     */
    fun <TaskT: DefaultTask> transform(
            artifactType: SingleFileArtifactType,
            taskProvider: TaskProvider<TaskT>,
            inputProvider: (TaskT) -> RegularFileProperty,
            outputProvider: (TaskT) -> RegularFileProperty
    )

    /**
     * Transforms an artifact.
     *
     * This configures the task input and output.
     *
     * The task must be able to consume more that one file and merge all the output in a single file.
     *
     * @param artifactType the type of the artifact to transform
     * @param taskProvider the provider for the task doing the transform
     * @param inputProvider the lambda returning the input field of the task
     * @param outputProvider the lambda returning the output field of the task
     *
     */
    fun <TaskT: DefaultTask> transform(
            artifactType : MultiFileArtifactType,
            taskProvider: TaskProvider<TaskT>,
            inputProvider: (TaskT) -> ListProperty<RegularFile>,
            outputProvider: (TaskT) -> RegularFileProperty
    )

    /**
     * Transforms an artifact.
     *
     * This configures the task input and output.
     *
     * The task must be able to consume more that one file and merge all the output in a single file.
     *
     * @param artifactType the type of the artifact to transform
     * @param taskProvider the provider for the task doing the transform
     * @param inputProvider the lambda returning the input field of the task
     * @param outputProvider the lambda returning the output field of the task
     *
     */
    fun <TaskT: DefaultTask> transform(
            artifactType : MultiDirectoryArtifactType,
            taskProvider: TaskProvider<TaskT>,
            inputProvider: (TaskT) -> ListProperty<Directory>,
            outputProvider: (TaskT) -> DirectoryProperty
    )

    /**
     * Appends a task-generated Directory to an artifact.
     *
     * This configures the task output.
     *
     * Appends always happen before any transforms. Transforms are guaranteed to see all the appended outputs.
     *
     * @param artifactType the type of the artifact to transform
     * @param taskProvider the provider for the task doing the append
     * @param outputProvider the lambda returning the output field of the task
     *
     */
    fun <TaskT: DefaultTask> append(
            artifactType : MultiDirectoryArtifactType,
            taskProvider: TaskProvider<TaskT>,
            outputProvider: (TaskT) -> DirectoryProperty
    )

    /**
     * Appends a task-generated File to an artifact.
     *
     * This configures the task output.
     *
     * Appends always happen before any transforms. Transforms are guaranteed to see all the appended outputs.
     *
     * @param artifactType the type of the artifact to transform
     * @param taskProvider the provider for the task doing the append
     * @param outputProvider the lambda returning the output field of the task
     *
     */
    fun <TaskT: DefaultTask> append(
            artifactType : MultiFileArtifactType,
            taskProvider: TaskProvider<TaskT>,
            outputProvider: (TaskT) -> RegularFileProperty
    )

    /**
     * Appends a task-generated or folder to a mixed artifact.
     *
     * This configures the task output.
     *
     * Appends always happen before any transforms. Transforms are guaranteed to see all the appended outputs.
     *
     * @param artifactType the type of the artifact to transform
     * @param taskProvider the provider for the task doing the append
     * @param outputProvider the lambda returning the output field of the task
     *
     */
    fun <TaskT : DefaultTask> append(
            artifactType : MultiMixedArtifactType,
            taskProvider: TaskProvider<TaskT>,
            outputProvider: (TaskT) -> Property<out FileSystemLocation>
    )
}

interface ArtifactHandler<TaskT> where TaskT: DefaultTask {

    fun <ValueT: FileSystemLocation, ProviderT: Provider<ValueT>> input(
            artifactType: SingleArtifactType<ValueT, ProviderT>,
            inputProvider: (TaskT) -> Property<ValueT>
    ): ArtifactHandler<TaskT>

    fun <ValueT: FileSystemLocation, ProviderT: Provider<out Iterable<ValueT>>> input(
            artifactType: MultiArtifactType<ValueT, ProviderT>,
            inputProvider: (TaskT) -> HasMultipleValues<ValueT>
    ): ArtifactHandler<TaskT>
}

//----------
// Base interfaces for input and output of tasks created with append/transform

interface DirectoryProducerTask: ArtifactProducer<Directory> {
    @get:OutputDirectory
    override val outputArtifact: DirectoryProperty
}

interface DirectoryConsumerTask: ArtifactConsumer<Directory> {
    override val inputArtifact: DirectoryProperty
}

interface FileProducerTask: ArtifactProducer<RegularFile> {
    @get:OutputFile
    override val outputArtifact: RegularFileProperty
}

interface FileListConsumerTask : ArtifactListConsumer<RegularFile> {
    override val inputArtifacts: ListProperty<RegularFile>
}

// -------
// Parent interfaces

interface ArtifactProducer<ValueT: FileSystemLocation> {
    @get:Internal
    val outputArtifact: Property<ValueT>
}

interface ArtifactConsumer<ValueT: FileSystemLocation> {
    @get:Internal
    val inputArtifact: Property<ValueT>
}

interface ArtifactListConsumer<ValueT: FileSystemLocation> {
    @get:Internal
    val inputArtifacts: ListProperty<ValueT>
}
