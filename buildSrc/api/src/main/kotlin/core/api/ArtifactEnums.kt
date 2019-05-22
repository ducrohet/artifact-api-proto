@file:Suppress("UnstableApiUsage")
package core.api

import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
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
    val sensitivity: PathSensitivity
    val normalizer: Class<out FileNormalizer>?
}

/**
 * Base for artifact only handling a single file or directory
 */
interface SingleArtifactType<ValueT, ProviderT: Provider<ValueT>> : InputInfoProvider

/**
 * Base for artifacts being able to handle either a list of file or a list of directory.
 */
interface MultiArtifactType<ValueT, ProviderT: Provider<out Iterable<ValueT>>> : InputInfoProvider

/**
 * Actual enum classes.
 *
 * Separated in 4 categories, as combinations of:
 * - file or directory
 * - single or multiple
 */

enum class SingleFileArtifactType(
        override val sensitivity: PathSensitivity,
        override val normalizer: Class<out FileNormalizer>? = null
): SingleArtifactType<RegularFile, Provider<RegularFile>> {
    MANIFEST(PathSensitivity.NONE);
}

enum class SingleDirectoryArtifactType(
        override val sensitivity: PathSensitivity,
        override val normalizer: Class<out FileNormalizer>? = null
): SingleArtifactType<Directory, Provider<Directory>> {
    MERGED_RESOURCES(PathSensitivity.NONE);
}

enum class MultiFileArtifactType(
        override val sensitivity: PathSensitivity,
        override val normalizer: Class<out FileNormalizer>? = null
): MultiArtifactType<RegularFile, Provider<out Iterable<RegularFile>>> {
    DEX(PathSensitivity.NONE);
}

enum class MultiDirectoryArtifactType(
        override val sensitivity: PathSensitivity,
        override val normalizer: Class<out FileNormalizer>? = null
): MultiArtifactType<Directory, Provider<Iterable<Directory>>> {
}
