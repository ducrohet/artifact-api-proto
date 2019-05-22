package core.api

import org.gradle.api.Plugin
import org.gradle.api.Project

class PublicPlugin: Plugin<Project> {

    override fun apply(project: Project) {
        project.apply(mapOf<String, String>("plugin" to "core.internal.plugin"))
    }
}