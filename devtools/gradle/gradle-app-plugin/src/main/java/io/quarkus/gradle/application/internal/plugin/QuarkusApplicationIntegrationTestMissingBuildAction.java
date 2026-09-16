package io.quarkus.gradle.application.internal.plugin;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Task;

final class QuarkusApplicationIntegrationTestMissingBuildAction implements Action<Task> {

    private final String suiteName;
    private final String buildReference;
    private boolean attached;

    QuarkusApplicationIntegrationTestMissingBuildAction(String suiteName, String buildReference) {
        this.suiteName = suiteName;
        this.buildReference = buildReference;
    }

    void attach() {
        attached = true;
    }

    @Override
    public void execute(Task task) {
        if (attached) {
            return;
        }
        throw new GradleException("Quarkus integration-test suite '" + suiteName
                + "' references application build '" + buildReference + "', but no matching runnable build was registered");
    }
}
