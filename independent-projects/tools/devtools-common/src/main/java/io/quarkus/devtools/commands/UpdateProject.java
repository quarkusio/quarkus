package io.quarkus.devtools.commands;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.commands.handlers.UpdateProjectCommandHandler;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.registry.catalog.ExtensionCatalog;

/**
 * Instances of this class are not thread-safe. They are created per single invocation.
 */
public class UpdateProject {

    public static final String APP_MODEL = "quarkus.update-project.app-model";
    public static final String LATEST_CATALOG = "quarkus.update-project.latest-catalog";
    public static final String PER_MODULE = "quarkus.update-project.per-module";

    public static final String GENERATE_REWRITE_CONFIG = "quarkus.update-project.generate-update-config";
    public static final String TARGET_PLATFORM_VERSION = "quarkus.update-project.target-platform-version";

    private final QuarkusCommandInvocation invocation;
    private final UpdateProjectCommandHandler handler = new UpdateProjectCommandHandler();

    public UpdateProject(final QuarkusProject quarkusProject) {
        this.invocation = new QuarkusCommandInvocation(quarkusProject);
    }

    public UpdateProject(final QuarkusProject quarkusProject, final MessageWriter messageWriter) {
        this.invocation = new QuarkusCommandInvocation(quarkusProject, new HashMap<>(), messageWriter);
    }

    public UpdateProject latestCatalog(ExtensionCatalog latestCatalog) {
        invocation.setValue(LATEST_CATALOG, requireNonNull(latestCatalog, "latestCatalog is required"));
        return this;
    }

    public UpdateProject appModel(ApplicationModel applicationModel) {
        invocation.setValue(APP_MODEL, requireNonNull(applicationModel, "applicationModel is required"));
        return this;
    }

    public UpdateProject generateRewriteConfig(boolean generateUpdateConfig) {
        invocation.setValue(GENERATE_REWRITE_CONFIG, generateUpdateConfig);
        return this;
    }

    public UpdateProject perModule(boolean perModule) {
        invocation.setValue(PER_MODULE, perModule);
        return this;
    }

    public UpdateProject targetPlatformVersion(String targetPlatformVersion) {
        invocation.setValue(TARGET_PLATFORM_VERSION, targetPlatformVersion);
        return this;
    }

    public QuarkusCommandOutcome execute() throws QuarkusCommandException {
        return handler.execute(invocation);
    }
}
