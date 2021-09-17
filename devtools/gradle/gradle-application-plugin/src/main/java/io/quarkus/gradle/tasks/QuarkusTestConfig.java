package io.quarkus.gradle.tasks;

import org.gradle.api.tasks.TaskAction;

public class QuarkusTestConfig extends QuarkusTask {

    public QuarkusTestConfig() {
        super("Deprecated. Used to set the necessary system properties for the Quarkus tests to run. Replaced with an action configured for every test by the Quarkus plugin");
    }

    @TaskAction
    public void setupTest() {
        getLogger().warn(getPath() + " is deprecated, it's effectively a no-op now");
    }
}
