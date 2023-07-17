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
    public static final String TARGET_CATALOG = "quarkus.update-project.target-catalog";
    public static final String PER_MODULE = "quarkus.update-project.per-module";
    public static final String NO_REWRITE = "quarkus.update-project.rewrite.disabled";
    public static final String TARGET_PLATFORM_VERSION = "quarkus.update-project.target-platform-version";
    public static final String REWRITE_PLUGIN_VERSION = "quarkus.update-project.rewrite.plugin-version";
    public static final String REWRITE_UPDATE_RECIPES_VERSION = "quarkus.update-project.rewrite.update-recipes-version";
    public static final String REWRITE_DRY_RUN = "quarkus.update-project.rewrite.dry-run";

    private final QuarkusCommandInvocation invocation;
    private final UpdateProjectCommandHandler handler = new UpdateProjectCommandHandler();

    public UpdateProject(final QuarkusProject quarkusProject) {
        this.invocation = new QuarkusCommandInvocation(quarkusProject);
    }

    public UpdateProject(final QuarkusProject quarkusProject, final MessageWriter messageWriter) {
        this.invocation = new QuarkusCommandInvocation(quarkusProject, new HashMap<>(), messageWriter);
    }

    public UpdateProject targetCatalog(ExtensionCatalog latestCatalog) {
        invocation.setValue(TARGET_CATALOG, requireNonNull(latestCatalog, "targetCatalog is required"));
        return this;
    }

    public UpdateProject appModel(ApplicationModel applicationModel) {
        invocation.setValue(APP_MODEL, requireNonNull(applicationModel, "applicationModel is required"));
        return this;
    }

    public UpdateProject noRewrite(boolean noRewrite) {
        invocation.setValue(NO_REWRITE, noRewrite);
        return this;
    }

    public UpdateProject rewritePluginVersion(String rewritePluginVersion) {
        invocation.setValue(REWRITE_PLUGIN_VERSION, requireNonNull(rewritePluginVersion, "rewritePluginVersion is required"));
        return this;
    }

    public UpdateProject rewriteUpdateRecipesVersion(String rewriteUpdateRecipesVersion) {
        invocation.setValue(REWRITE_UPDATE_RECIPES_VERSION,
                requireNonNull(rewriteUpdateRecipesVersion, "rewriteUpdateRecipesVersion is required"));
        return this;
    }

    public UpdateProject rewriteDryRun(boolean rewriteDryRun) {
        invocation.setValue(REWRITE_DRY_RUN, rewriteDryRun);
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
