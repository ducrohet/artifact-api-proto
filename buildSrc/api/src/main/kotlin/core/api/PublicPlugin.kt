package core.api

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Empty plugin that does nothing, except apply an internal plugin.
 */
class PublicPlugin: Plugin<Project> {

    override fun apply(project: Project) {
        project.apply(mapOf<String, String>("plugin" to "core.internal.plugin"))
    }
}