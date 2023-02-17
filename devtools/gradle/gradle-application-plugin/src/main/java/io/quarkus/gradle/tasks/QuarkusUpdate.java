package io.quarkus.gradle.tasks;

import java.util.List;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import io.quarkus.devtools.commands.UpdateProject;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.PlatformStreamCoords;

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
    @Optional
    public String getTargetStreamId() {
        return targetStreamId;
    }

    @Option(description = "A target stream id, for example:  2.0", option = "streamId")
    public void setStreamId(String targetStreamId) {
        this.targetStreamId = targetStreamId;
    }

    @Input
    @Optional
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
        final ExtensionCatalog targetCatalog;
        try {
            if (targetPlatformVersion != null) {
                var targetPrimaryBom = getPrimaryBom(quarkusProject.getExtensionsCatalog());
                targetPrimaryBom = ArtifactCoords.pom(targetPrimaryBom.getGroupId(), targetPrimaryBom.getArtifactId(),
                        targetPlatformVersion);
                targetCatalog = getExtensionCatalogResolver(quarkusProject.log())
                        .resolveExtensionCatalog(List.of(targetPrimaryBom));
            } else if (targetStreamId != null) {
                var platformStream = PlatformStreamCoords.fromString(targetStreamId);
                targetCatalog = getExtensionCatalogResolver(quarkusProject.log()).resolveExtensionCatalog(platformStream);
                targetPlatformVersion = getPrimaryBom(targetCatalog).getVersion();
            } else {
                targetCatalog = getExtensionCatalogResolver(quarkusProject.log()).resolveExtensionCatalog();
                targetPlatformVersion = getPrimaryBom(targetCatalog).getVersion();
            }
        } catch (RegistryResolutionException e) {
            throw new RuntimeException(
                    "Failed to resolve the recommended Quarkus extension catalog from the configured extension registries", e);
        }

        final UpdateProject invoker = new UpdateProject(quarkusProject);
        invoker.latestCatalog(targetCatalog);
        invoker.targetPlatformVersion(targetPlatformVersion);
        invoker.perModule(perModule);
        invoker.appModel(extension().getApplicationModel());
        try {
            invoker.execute();
        } catch (Exception e) {
            throw new GradleException("Failed to resolve recommended updates", e);
        }
    }

    private static ArtifactCoords getPrimaryBom(ExtensionCatalog c) {
        return c.getDerivedFrom().isEmpty() ? c.getBom() : c.getDerivedFrom().get(0).getBom();
    }
}
