@file:Suppress("UnstableApiUsage")
package core.api

import org.gradle.api.file.Directory
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.ClasspathNormalizer
import org.gradle.api.tasks.CompileClasspathNormalizer
import org.gradle.api.tasks.FileNormalizer
import org.gradle.api.tasks.PathSensitivity

/**
 * Provider of input information such as [PathSensitivity] and [FileNormalizer].
 *
 * This is attached to an artifact enum to automatically configure the inputs of tasks that
 * transform artifact
 *
 */
interface InputInfoProvider {
    val sensitivity: PathSensitivity?
    val normalizer: Class<out FileNormalizer>?
}

/**
 * Base for artifacts only handling a single file or directory
 */
interface SingleArtifactType<ValueT: FileSystemLocation, ProviderT: Provider<ValueT>> : InputInfoProvider {
    val isOutput: Boolean
}

/**
 * Base for artifacts being able to handle either a list of file or a list of directory.
 */
interface MultiArtifactType<ValueT: FileSystemLocation, ProviderT: Provider<out Iterable<ValueT>>> : InputInfoProvider

/**
 * Actual enum classes.
 *
 * Separated in 4 categories, as combinations of:
 * - file or directory
 * - single or multiple
 */

enum class SingleFileArtifactType(
        override val sensitivity: PathSensitivity? = null,
        override val normalizer: Class<out FileNormalizer>? = null,
        override val isOutput: Boolean = false
): SingleArtifactType<RegularFile, Provider<RegularFile>> {
    MERGED_MANIFEST(sensitivity = PathSensitivity.NAME_ONLY),
    MERGED_DEX(sensitivity = PathSensitivity.NAME_ONLY),
    PACKAGE(sensitivity = PathSensitivity.NONE, isOutput = true)
}

enum class SingleDirectoryArtifactType(
        override val sensitivity: PathSensitivity? = null,
        override val normalizer: Class<out FileNormalizer>? = null,
        override val isOutput: Boolean = false
): SingleArtifactType<Directory, Provider<Directory>> {
    MERGED_RESOURCES(sensitivity= PathSensitivity.NONE);
}

enum class MultiFileArtifactType(
        override val sensitivity: PathSensitivity? = null,
        override val normalizer: Class<out FileNormalizer>? = null
): MultiArtifactType<RegularFile, Provider<out Iterable<RegularFile>>> {
    COMPILE_R_JAR(normalizer = ClasspathNormalizer::class.java),
    RUNTIME_R_JAR(normalizer = CompileClasspathNormalizer::class.java),
    DEX(sensitivity = PathSensitivity.NONE);
}

enum class MultiDirectoryArtifactType(
        override val sensitivity: PathSensitivity? = null,
        override val normalizer: Class<out FileNormalizer>? = null
): MultiArtifactType<Directory, Provider<out Iterable<Directory>>> {
    RESOURCES(sensitivity= PathSensitivity.NONE);
}

enum class MultiMixedArtifactType(
        override val sensitivity: PathSensitivity? = null,
        override val normalizer: Class<out FileNormalizer>? = null
): MultiArtifactType<FileSystemLocation, Provider<out Iterable<FileSystemLocation>>> {
    BYTECODE(normalizer = CompileClasspathNormalizer::class.java);
}
