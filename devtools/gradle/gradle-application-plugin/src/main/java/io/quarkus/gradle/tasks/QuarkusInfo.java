package io.quarkus.gradle.tasks;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.devtools.commands.ProjectInfo;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.project.QuarkusProject;

public abstract class QuarkusInfo extends QuarkusPlatformTask {

    private boolean perModule = false;

    @Input
    public boolean getPerModule() {
        return perModule;
    }

    @Option(description = "Log project's state per module.", option = "perModule")
    public void setPerModule(boolean perModule) {
        this.perModule = perModule;
    }

    public QuarkusInfo() {
        super("Log Quarkus-specific project information, such as imported Quarkus platform BOMs, Quarkus extensions found among the project dependencies, etc.");
    }

    @TaskAction
    public void logInfo() {
        getLogger().warn(getName() + " is experimental, its options and output might change in future versions");

        final QuarkusProject quarkusProject = getQuarkusProject(false);
        final QuarkusCommandOutcome outcome;
        final ProjectInfo invoker = new ProjectInfo(quarkusProject);
        invoker.perModule(perModule);
        invoker.appModel(extension().getApplicationModel());
        try {
            outcome = invoker.execute();
        } catch (Exception e) {
            throw new GradleException("Failed to collect Quarkus project information", e);
        }
    }
}
