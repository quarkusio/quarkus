package io.quarkus.gradle.application.internal.plugin;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.tasks.testing.Test;

final class QuarkusApplicationStartupArchiveTestWorkerValidationAction implements Action<Task> {

    private final String suiteName;

    QuarkusApplicationStartupArchiveTestWorkerValidationAction(String suiteName) {
        this.suiteName = suiteName;
    }

    @Override
    public void execute(Task task) {
        Test test = (Test) task;
        if (test.getMaxParallelForks() != 1 || test.getForkEvery() != 0) {
            throw new GradleException("Quarkus integration-test suite '" + suiteName
                    + "' uses startup-archive training and must use maxParallelForks=1 and forkEvery=0");
        }
    }
}
