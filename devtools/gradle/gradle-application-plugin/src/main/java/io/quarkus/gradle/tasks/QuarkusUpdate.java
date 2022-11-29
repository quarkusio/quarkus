package io.quarkus.gradle.tasks;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.devtools.commands.UpdateProject;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.registry.RegistryResolutionException;

public class QuarkusUpdate extends QuarkusPlatformTask {

    private boolean perModule = false;
    private String targetStreamId;
    private String targetPlatformVersion;

    @Input
    public boolean getPerModule() {
        return perModule;
    }

    @Option(description = "Log project's state per module.", option = "perModule")
    public void setPerModule(boolean perModule) {
        this.perModule = perModule;
    }

    @Input
    public String getTargetStreamId() {
        return targetStreamId;
    }

    @Option(description = "A target stream id, for example:  2.0", option = "streamId")
    public void setStreamId(String targetStreamId) {
        this.targetStreamId = targetStreamId;
    }

    @Input
    public String getTargetPlatformVersion() {
        return targetPlatformVersion;
    }

    @Option(description = "A target platform version, for example:  2.0.0.Final", option = "platformVersion")
    public void setTargetPlatformVersion(String targetPlatformVersion) {
        this.targetPlatformVersion = targetPlatformVersion;
    }

    public QuarkusUpdate() {
        super("Log Quarkus-specific recommended project updates, such as the new Quarkus platform BOM versions, new versions of Quarkus extensions that aren't managed by the Quarkus BOMs, etc");
    }

    @TaskAction
    public void logUpdates() {

        getProject().getLogger().warn(getName() + " is experimental, its options and output might change in future versions");

        final QuarkusProject quarkusProject = getQuarkusProject(false);
        final UpdateProject invoker = new UpdateProject(quarkusProject);
        try {
            invoker.latestCatalog(getExtensionCatalogResolver(quarkusProject.log()).resolveExtensionCatalog());
        } catch (RegistryResolutionException e) {
            throw new GradleException(
                    "Failed to resolve the latest Quarkus extension catalog from the configured extension registries", e);
        }

        // TODO ALEXEY: resolve targetPlatformVersion from targetPlatformStreamId if needed or from latest version
        invoker.targetPlatformVersion(targetPlatformVersion);
        invoker.perModule(perModule);
        invoker.appModel(extension().getApplicationModel());
        try {
            invoker.execute();
        } catch (Exception e) {
            throw new GradleException("Failed to resolve recommended updates", e);
        }
    }
}
