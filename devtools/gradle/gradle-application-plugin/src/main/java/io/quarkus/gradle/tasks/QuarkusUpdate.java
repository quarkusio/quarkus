package io.quarkus.gradle.tasks;

import java.util.HashMap;
import java.util.Map;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.handlers.UpdateCommandHandler;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.registry.RegistryResolutionException;

public class QuarkusUpdate extends QuarkusPlatformTask {

    private boolean perModule = false;
    private boolean recommendedState = false;
    private boolean rectify = false;

    @Input
    public boolean getPerModule() {
        return perModule;
    }

    @Option(description = "Log project's state per module.", option = "perModule")
    public void setPerModule(boolean perModule) {
        this.perModule = perModule;
    }

    @Input
    public boolean getRecommendedState() {
        return recommendedState;
    }

    @Option(description = "Log the new recommended project state.", option = "recommendedState")
    public void setRecommendedState(boolean recommendedState) {
        this.recommendedState = recommendedState;
    }

    @Input
    public boolean getRectify() {
        return rectify;
    }

    @Option(description = "Log the rectified state of the current project according to the recommendations based on the currently enforced Quarkus platforms.", option = "rectify")
    public void setRectify(boolean rectify) {
        this.rectify = rectify;
    }

    public QuarkusUpdate() {
        super("Log Quarkus-specific recommended project updates, such as the new Quarkus platform BOM versions, new versions of Quarkus extensions that aren't managed by the Quarkus BOMs, etc");
    }

    @TaskAction
    public void logUpdates() {

        getProject().getLogger().warn(getName() + " is experimental, its options and output might change in future versions");

        final QuarkusProject quarkusProject = getQuarkusProject(false);
        final Map<String, Object> params = new HashMap<>();
        try {
            params.put(UpdateCommandHandler.LATEST_CATALOG,
                    getExtensionCatalogResolver(quarkusProject.log()).resolveExtensionCatalog());
        } catch (RegistryResolutionException e) {
            throw new GradleException(
                    "Failed to resolve the latest Quarkus extension catalog from the configured extension registries", e);
        }
        params.put(UpdateCommandHandler.APP_MODEL, extension().getApplicationModel());
        params.put(UpdateCommandHandler.LOG_STATE_PER_MODULE, perModule);
        params.put(UpdateCommandHandler.LOG_RECOMMENDED_STATE, recommendedState);
        params.put(UpdateCommandHandler.RECTIFY, rectify);
        final QuarkusCommandInvocation invocation = new QuarkusCommandInvocation(quarkusProject, params);
        try {
            new UpdateCommandHandler().execute(invocation);
        } catch (Exception e) {
            throw new GradleException("Failed to resolve recommended updates", e);
        }
    }
}
