@file:Suppress("UNCHECKED_CAST", "UnstableApiUsage")

package custom.plugin.api

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*

/**
 * Base API for manipulating artifacts.
 *
 * Contains method to read, transform and append to artifacts.
 */
interface ArtifactHolder {
    /**
     * Returns an artifact as a [Provider] of [RegularFile] or [Directory]
     * This is compatible with [RegularFileProperty] and [DirectoryProperty]
     *
     * This is always the final version of this artifact. This is to be used for task wanting to consume an artifact.
     *
     * @param artifactType the type of the artifact
     * @return the artifact as a provider containing both value and task dependency information
     */
    fun <ValueT, ProviderT: Provider<ValueT>> getArtifact(
            artifactType : SingleArtifactType<ValueT, ProviderT>
    ) : Provider<ValueT>

    /**
     * Returns an artifact as a [Provider] of iterable [RegularFile] or [Directory]
     * This is compatible with [ListProperty]
     *
     * This is always the final version of this artifact. This is to be used for task wanting to consume an artifact.
     *
     * @param artifactType the type of the artifact
     * @return the artifact as a provider containing both value and task dependency information
     */
    fun <ValueT, ProviderT: Provider<out Iterable<ValueT>>> getArtifact(
            artifactType : MultiArtifactType<ValueT, ProviderT>
    ) : Provider<out Iterable<ValueT>>

    /**
     * INTERNAL ONLY
     *
     * This sets the initial value of an artifact. This should not be a public API, but instead used by the internal
     * task generating the original value.
     */
    fun <ValueT, ProviderT: Provider<ValueT>> produces(
            artifactType : SingleArtifactType<ValueT, ProviderT>,
            artifact: Provider<ValueT>
    )

    /**
     * INTERNAL ONLY
     *
     * This sets the initial value of an artifact. This should not be a public API, but instead used by the internal
     * task generating the original value.
     */
    fun <ValueT, ProviderT: Provider<out Iterable<ValueT>>> produces(
            artifactType : MultiArtifactType<ValueT, ProviderT>,
            artifact: Provider<ValueT>
    )

    /**
     * Transforms an artifact.
     *
     * This registers the task of the given type and wires its input and output (based on [DirectoryConsumerTask] and
     * [DirectoryProducerTask])
     *
     * @param artifactType the type of the artifact to transform
     * @param taskName the name of the task to register
     * @param taskClass the type of the task to register.
     * @param configAction a delayed config action to configure the task
     * @return a [TaskProvider] for the newly created task
     *
     */
    fun <TaskT> transform(
            artifactType: SingleDirectoryArtifactType,
            taskName: String,
            taskClass: Class<TaskT>,
            configAction: (TaskT) -> Unit
    ): TaskProvider<TaskT> where TaskT: DefaultTask, TaskT : DirectoryConsumerTask, TaskT : DirectoryProducerTask

    /**
     * Transforms an artifact.
     *
     * This registers the task of the given type and wires its input and output (based on [FileConsumerTask] and
     * [FileProducerTask])
     *
     * @param artifactType the type of the artifact to transform
     * @param taskName the name of the task to register
     * @param taskClass the type of the task to register.
     * @param configAction a delayed config action to configure the task
     * @return a [TaskProvider] for the newly created task
     *
     */
    fun <TaskT> transform(
            artifactType: SingleFileArtifactType,
            taskName: String,
            taskClass: Class<TaskT>,
            configAction: (TaskT) -> Unit
    ): TaskProvider<TaskT> where TaskT: DefaultTask, TaskT : FileConsumerTask, TaskT : FileProducerTask

    /**
     * Transforms an artifact.
     *
     * This registers the task of the given type and wires its input and output (based on [FileListConsumerTask] and
     * [FileProducerTask])
     *
     * The task must be able to consume more that one file and merge all the output in a single file.
     *
     * @param artifactType the type of the artifact to transform
     * @param taskName the name of the task to register
     * @param taskClass the type of the task to register.
     * @param configAction a delayed config action to configure the task
     * @return a [TaskProvider] for the newly created task
     *
     */
    fun <TaskT> transform(
            artifactType : MultiFileArtifactType,
            taskName: String,
            taskClass: Class<TaskT>,
            configAction: (TaskT) -> Unit
    ) : TaskProvider<TaskT> where TaskT: DefaultTask, TaskT: FileListConsumerTask, TaskT: FileProducerTask

    /**
     * Appends a task-generated Directory to an artifact.
     *
     * This registers the task of the given type and wires its output (based on [DirectoryProducerTask])
     * The output is a single Directory.
     *
     * @param artifactType the type of the artifact to transform
     * @param taskName the name of the task to register
     * @param taskClass the type of the task to register.
     * @param configAction a delayed config action to configure the task
     * @return a [TaskProvider] for the newly created task
     *
     */
    fun <TaskT> append(
            artifactType : MultiDirectoryArtifactType,
            taskName: String,
            taskClass: Class<TaskT>,
            configAction: (TaskT) -> Unit
    ) : TaskProvider<TaskT> where TaskT: DefaultTask, TaskT: DirectoryProducerTask

    /**
     * Appends a task-generated File to an artifact.
     *
     * This registers the task of the given type and wires its output (based on [FileProducerTask])
     * The output is a single File.
     *
     * @param artifactType the type of the artifact to transform
     * @param taskName the name of the task to register
     * @param taskClass the type of the task to register.
     * @param configAction a delayed config action to configure the task
     * @return a [TaskProvider] for the newly created task
     *
     */
    fun <TaskT> append(
            artifactType : MultiFileArtifactType,
            taskName: String,
            taskClass: Class<TaskT>,
            configAction: (TaskT) -> Unit
    ) : TaskProvider<TaskT> where TaskT: DefaultTask, TaskT: FileProducerTask
}


//----------
// Base interfaces for input and output of tasks created with append/transform

interface DirectoryProducerTask: ArtifactProducer<Directory> {
    override val outputArtifact: DirectoryProperty
}

interface DirectoryConsumerTask: ArtifactConsumer<Directory> {
    override val inputArtifact: DirectoryProperty
}

interface FileProducerTask: ArtifactProducer<RegularFile> {
    override val outputArtifact: RegularFileProperty
}

interface FileConsumerTask: ArtifactConsumer<RegularFile> {
    override val inputArtifact: RegularFileProperty
}

interface FileListConsumerTask : ArtifactListConsumer<RegularFile> {
    override val inputArtifacts: ListProperty<RegularFile>
}

// -------
// Parent interfaces

interface ArtifactProducer<ValueT> {
    @get:Internal
    val outputArtifact: Property<ValueT>
}

interface ArtifactConsumer<ValueT> {
    @get:Internal
    val inputArtifact: Property<ValueT>
}

interface ArtifactListConsumer<ValueT> {
    @get:Internal
    val inputArtifacts: ListProperty<ValueT>
}
