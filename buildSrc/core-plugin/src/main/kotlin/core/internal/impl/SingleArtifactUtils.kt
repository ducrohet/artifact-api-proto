@file:Suppress("UnstableApiUsage")
package core.internal.impl

import core.api.*
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.*
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskInputFilePropertyBuilder
import org.gradle.api.tasks.TaskProvider
import java.io.File
import java.util.*

class SingleDirectoryHolder(
        project: Project
): SingleArtifactHolder<SingleDirectoryArtifactType, Directory, Provider<Directory>>(project) {

    override val map = EnumMap<SingleDirectoryArtifactType, SingleArtifactInfo<Directory>>(SingleDirectoryArtifactType::class.java)

    override fun newProperty(): Property<Directory> = project.objects.directoryProperty()

    override fun newIntermediateProperty(artifactType: SingleDirectoryArtifactType, taskName: String): DirectoryProperty =
            project.objects.directoryProperty().also {
                it.set(newIntermediateLocation(artifactType, taskName))
            }

    override fun newIntermediateLocation(artifactType: SingleDirectoryArtifactType, taskName: String) =
            File(project.buildDir, "intermediates/$taskName/$artifactType")

    override fun newOutputLocation(artifactType: SingleDirectoryArtifactType): File =
            File(project.buildDir, "outputs/${artifactType.toString().toLowerCase()}")

    init {
        for (artifact in SingleDirectoryArtifactType.values()) {
            init(artifact)
        }
    }

    fun finalizeLocations() {
        SingleDirectoryArtifactType.values().filter { it.isOutput }.forEach {
            finalizeLocation(it)
        }
    }
}

class SingleFileHolder(
        project: Project
): SingleArtifactHolder<SingleFileArtifactType, RegularFile, Provider<RegularFile>>(project) {

    override val map = EnumMap<SingleFileArtifactType, SingleArtifactInfo<RegularFile>>(SingleFileArtifactType::class.java)

    override fun newProperty(): Property<RegularFile> = project.objects.fileProperty()
    override fun newIntermediateProperty(artifactType: SingleFileArtifactType, taskName: String): RegularFileProperty =
            project.objects.fileProperty().also {
                it.set(newIntermediateLocation(artifactType, taskName))
            }

    override fun newIntermediateLocation(artifactType: SingleFileArtifactType, taskName: String) =
            File(project.buildDir, "intermediates/$taskName/$artifactType.txt")

    override fun newOutputLocation(artifactType: SingleFileArtifactType): File =
            File(project.buildDir, "outputs/${artifactType.toString().toLowerCase()}.txt")

    init {
        for (artifact in SingleFileArtifactType.values()) {
            init(artifact)
        }
    }

    fun finalizeLocations() {
        SingleFileArtifactType.values().filter { it.isOutput }.forEach {
            finalizeLocation(it)
        }
    }
}

/**
 * class that holds the info for a given artifact.
 *
 * It contains references to the current last version of the artifact, a dynamic final artifact property, etc..
 */
class SingleArtifactInfo<ValueT: FileSystemLocation>(
        propertyProvider: () -> Property<ValueT>
) {
    /**
     * Wrapper around the first provider. This is used to handle transforms while the task that produce the first
     * version of the artifact has not yet been set.
     */
    private val firstArtifact: Property<ValueT> = propertyProvider()

    /**
     * Final Artifact. Always the final value of the artifact. This is dynamic and is updated as new transforms
     * are added.
     */
    val finalArtifact: Property<ValueT> = propertyProvider()

    /**
     * whether the very first artifact version was generated
     */
    var isInitialized: Boolean = false
        private set

    /**
     * Whether transforms have been set on the artifact
     */
    var hasTransforms: Boolean = false
        private set

    /**
     * The current artifact version. Every new transform updates this to the output of the new transform
     *
     * @see [SingleArtifactInfo.setNewOutput]
     */
    private var currentArtifact: Provider<ValueT> = firstArtifact

    /**
     * an optional final artifact location. Use when the artifact type is an output (see [SingleArtifactType.isOutput])
     *
     * This is used as the source for currentArtifact.
     * Instead of doing <code>currentArtifact.set(File)</code> we do
     * <code>
     *     finalArtifactLocation = project.objects.directoryProperty()
     *     finalArtifactLocation.set(File)
     *     currentArtifact.set(finalArtifactLocation)
     * </code>
     *
     * Later, we'll go back and edit the location of finalArtifactProvider which is the wrapper on
     * the location of the last task transforming the artifact.
     */
    lateinit var finalArtifactLocation: Property<ValueT>

    /**
     * indicate whether there is a finalArtifactLocation.
     *
     * This generally returns false if the artifact is not an output
     */
    val isFinalLocationInitialized: Boolean
        get() = ::finalArtifactLocation.isInitialized

    /**
     * Sets the value of the first artifact.
     * This can only be called once.
     */
    fun setFirstArtifact(artifact: Provider<ValueT>) {
        firstArtifact.set(artifact)
        isInitialized = true
    }

    /**
     * Sets a new output and return the old one
     */
    fun setNewOutput(artifact: Provider<ValueT>): Provider<ValueT> {
        hasTransforms = true

        val oldCurrent = currentArtifact
        finalArtifact.set(artifact)
        currentArtifact = artifact

        return oldCurrent
    }

    init {
        finalArtifact.set(currentArtifact)
    }
}

abstract class SingleArtifactHolder<ArtifactT: SingleArtifactType<ValueT, ProviderT>, ValueT: FileSystemLocation, ProviderT : Provider<ValueT>>(
        protected val project: Project
) {

    protected abstract val map: MutableMap<ArtifactT, SingleArtifactInfo<ValueT>>

    /**
     * Returns a new Property object of the right type.
     */
    protected abstract fun newProperty(): Property<ValueT>

    /**
     * Returns a new property, initialized with an intermediate location for the given artifact and task name
     */
    protected abstract fun newIntermediateProperty(artifactType: ArtifactT, taskName: String): Property<ValueT>
    /**
     * Returns an intermediate location for the given artifact and task name
     */
    protected abstract fun newIntermediateLocation(artifactType: ArtifactT, taskName: String): File
    /**
     * Returns an intermediate location for the given artifact and task name
     */
    protected abstract fun newOutputLocation(artifactType: ArtifactT): File

    protected fun init(artifactType: ArtifactT) {
        map[artifactType] = SingleArtifactInfo(::newProperty)
    }

    fun getArtifact(artifactType : ArtifactT) : Provider<ValueT> =
            map[artifactType]?.finalArtifact ?: throw RuntimeException("Did not find artifact for $artifactType")

    fun hasTransforms(artifactType : ArtifactT) : Boolean {
        val info = map[artifactType] ?: throw RuntimeException("Did not find artifact for $artifactType")
        return info.hasTransforms
    }

    fun <TaskT: DefaultTask> produces(
            artifactType: ArtifactT,
            taskProvider: TaskProvider<TaskT>,
            outputProvider: (TaskT) -> Property<ValueT>
    ) {
        val info = map[artifactType] ?: throw RuntimeException("Did not find artifact for $artifactType")
        if (info.isInitialized) {
            throw RuntimeException("Artifact $artifactType already initialized")
        }

        // set the first artifact wrapper to the actual output.
        info.setFirstArtifact(taskProvider.flatMap { outputProvider(it) })

        taskProvider.configure {
            // if the artifact is an output and there's no transforms on it, then this output
            // is the final output and should be in the output folder.
            // otherwise, it needs to go in intermediates.
            val location: File = if (artifactType.isOutput && !info.hasTransforms)
                newOutputLocation(artifactType)
            else
                newIntermediateLocation(artifactType, it.name)

            when (val output = outputProvider(it)) {
                is DirectoryProperty -> output.set(location)
                is RegularFileProperty -> output.set(location)
            }
        }
    }

    fun <TaskT> transform(
            artifactType : ArtifactT,
            taskName: String,
            taskClass: Class<TaskT>,
            configAction: (TaskT) -> Unit) : TaskProvider<TaskT> where TaskT: DefaultTask, TaskT: ArtifactConsumer<ValueT>, TaskT: ArtifactProducer<ValueT> {
        val info = map[artifactType] ?: throw RuntimeException("Did not find artifact for $artifactType")
        if (info.isInitialized) {
            throw RuntimeException("Cannot add transform on $artifactType. produces() already called")
        }

        val newTask = project.tasks.register(taskName, taskClass)

        // update the info with the new output and get the previous output. This will be used
        // to configure the input of the task
        val previousCurrent = info.setNewOutput(newTask.flatMap { it.outputArtifact} )

        // if the artifact is an output, then wrap the location of the task output in a property
        // that we keep track off. This way we can go back and set the final version to the outputs
        // folder.
        val outputLocationProperty: Property<ValueT>? =
                if (artifactType.isOutput) {
                    newIntermediateProperty(artifactType, taskName).also {
                        info.finalArtifactLocation = it
                    }
                } else {
                    null
                }


        newTask.configure {
            // set input value
            it.inputArtifact.set(previousCurrent)

            // register the inputs on the task, including setting the sensitivity and normalizer
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

            inputBuilder.withPropertyName("inputArtifact")
            if (artifactType.sensitivity != null) {
                @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                inputBuilder.withPathSensitivity(artifactType.sensitivity)
            } else if (artifactType.normalizer != null) {
                @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                inputBuilder.withNormalizer(artifactType.normalizer)
            }

            // set output location and register output
            if (outputLocationProperty != null) {
                processOutput(it, outputLocationProperty)
            } else {
                processOutput(it, newIntermediateLocation(artifactType, taskName))
            }

            // run the user's configuration action
            configAction.invoke(it)
        }

        return newTask
    }

    fun finalizeLocation(artifactType: ArtifactT) {
        val info = map[artifactType] ?: throw RuntimeException("Did not find artifact for $artifactType")

        if (info.isFinalLocationInitialized) {
            when (val location = info.finalArtifactLocation) {
                is DirectoryProperty -> location.set(newOutputLocation(artifactType))
                is RegularFileProperty -> location.set(newOutputLocation(artifactType))
            }
        }
    }
}
