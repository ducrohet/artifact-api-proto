package custom.plugin

import custom.plugin.api.ArtifactHolder
import custom.plugin.internal.api.ArtifactHolderImpl
import custom.plugin.internal.plugin.DirTaskCreator
import custom.plugin.internal.plugin.FileTaskCreator
import custom.plugin.internal.plugin.ListFileTaskCreator
import org.gradle.api.Plugin
import org.gradle.api.Project

class CustomPlugin: Plugin<Project> {
    lateinit var holder: ArtifactHolder

    override fun apply(project: Project) {
        holder = ArtifactHolderImpl(project)

        // create a bunch of test tasks.
        // task to tests are:
        // - finalDir, finalFile, finalFileList
        // use with -Pandroid.transform=true/false to enable/disable simulation of 3rd party tasks.

        DirTaskCreator(project, holder).create()
        FileTaskCreator(project, holder).create()
        ListFileTaskCreator(project, holder).create()
    }
}