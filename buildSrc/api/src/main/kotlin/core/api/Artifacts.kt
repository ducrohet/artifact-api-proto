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
 * Base for artifacts only handling a single file or directory
 */
interface SingleArtifactType<ValueT: FileSystemLocation, ProviderT: Provider<ValueT>> {
    val isOutput: Boolean
}

/**
 * Base for artifacts being able to handle either a list of file or a list of directory.
 */
interface MultiArtifactType<ValueT: FileSystemLocation, ProviderT: Provider<out Iterable<ValueT>>>

/**
 * Actual enum classes.
 *
 * Separated in 4 categories, as combinations of:
 * - file or directory
 * - single or multiple
 */

enum class SingleFileArtifactType(
        override val isOutput: Boolean = false
): SingleArtifactType<RegularFile, Provider<RegularFile>> {
    MERGED_MANIFEST,
    MERGED_DEX,
    PACKAGE
}

enum class SingleDirectoryArtifactType(
        override val isOutput: Boolean = false
): SingleArtifactType<Directory, Provider<Directory>> {
    MERGED_RESOURCES;
}

enum class MultiFileArtifactType : MultiArtifactType<RegularFile, Provider<out Iterable<RegularFile>>> {
    COMPILE_R_JAR,
    RUNTIME_R_JAR,
    DEX;
}

enum class MultiDirectoryArtifactType : MultiArtifactType<Directory, Provider<out Iterable<Directory>>> {
    RESOURCES;
}

enum class MultiMixedArtifactType : MultiArtifactType<FileSystemLocation, Provider<out Iterable<FileSystemLocation>>> {
    BYTECODE;
}
