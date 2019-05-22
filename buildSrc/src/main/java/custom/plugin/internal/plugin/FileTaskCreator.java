package custom.plugin.internal.plugin;

import custom.plugin.api.ArtifactHolder;
import custom.plugin.api.SingleFileArtifactType;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;

/**
 * Simple class creating tasks that produce, consume and transform files
 */
@SuppressWarnings("UnstableApiUsage")
public class FileTaskCreator {

    private final Project project;
    private final ArtifactHolder artifactHolder;

    public FileTaskCreator(Project project, ArtifactHolder artifactHolder) {

        this.project = project;
        this.artifactHolder = artifactHolder;
    }

    public void create() {
        // create transform first
        Object propValue = project.getProperties().get("android.transform");
        if (propValue != null && propValue.equals("true")) {
            createTransform();
        }

        // create final consumer
        project.getTasks().register(
                "finalFile",
                InternalFileConsumerTask.class,
                task -> task.getInputArtifact().set(artifactHolder.getArtifact(SingleFileArtifactType.MANIFEST)));

        // create the original producer last
        TaskProvider<InternalFileProducerTask> originTask = project.getTasks().register(
                "originFile",
                InternalFileProducerTask.class,
                task -> task.getOutputArtifact().set(new File(project.getBuildDir(), "foo.txt")));

        artifactHolder.produces(SingleFileArtifactType.MANIFEST, originTask.flatMap(InternalFileProducerTask::getOutputArtifact));
    }

    private void createTransform() {
        artifactHolder.transform(
                SingleFileArtifactType.MANIFEST,
                "transformFile",
                ExampleFileTransformerTask.class,
                task -> {
                    // do something
                    return null;
                }
        );
    }
}
