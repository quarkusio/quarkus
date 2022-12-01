package io.quarkus.gradle.tasks;

import java.util.HashMap;
import java.util.Map;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.commands.handlers.InfoCommandHandler;
import io.quarkus.devtools.commands.handlers.UpdateCommandHandler;
import io.quarkus.devtools.project.QuarkusProject;

public class QuarkusInfo extends QuarkusPlatformTask {

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

        getProject().getLogger().warn(getName() + " is experimental, its options and output might change in future versions");

        final QuarkusProject quarkusProject = getQuarkusProject(false);
        final Map<String, Object> params = new HashMap<>();
        params.put(UpdateCommandHandler.APP_MODEL, extension().getApplicationModel());
        params.put(UpdateCommandHandler.LOG_STATE_PER_MODULE, perModule);
        final QuarkusCommandInvocation invocation = new QuarkusCommandInvocation(quarkusProject, params);
        final QuarkusCommandOutcome outcome;
        try {
            outcome = new InfoCommandHandler().execute(invocation);
        } catch (Exception e) {
            throw new GradleException("Failed to collect Quarkus project information", e);
        }
        if (outcome.getValue(InfoCommandHandler.RECOMMENDATIONS_AVAILABLE, false)) {
            this.getProject().getLogger().warn(
                    "Non-recommended Quarkus platform BOM and/or extension versions were found. For more details, please, execute 'gradle quarkusUpdate --rectify'");
        }
    }
}
