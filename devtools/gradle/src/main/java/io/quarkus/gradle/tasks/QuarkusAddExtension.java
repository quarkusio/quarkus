package io.quarkus.gradle.tasks;

import org.gradle.api.tasks.TaskAction;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class QuarkusAddExtension extends QuarkusTask {

    public QuarkusAddExtension() {
        super("Adds one of the available quarkus extensions to the current project");
    }

    @TaskAction
    public void addExtension() {
        getLogger().lifecycle("Adding extension is not implemented yet");
    }

}
