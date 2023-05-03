package io.quarkus.maven;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import io.quarkus.devtools.commands.UpdateProject;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.PlatformStreamCoords;

/**
 * Log Quarkus-related recommended updates, such as new Quarkus platform BOM versions and
 * Quarkus extensions versions that aren't managed by the Quarkus platform BOMs.
 */
@Mojo(name = "update", requiresProject = true)
public class UpdateMojo extends QuarkusProjectStateMojoBase {

    /**
     * Display information per project module.
     */
    @Parameter(property = "perModule")
    boolean perModule;

    /**
     * If true, instead of checking and recommending the latest available Quarkus platform version,
     * recommendations to properly align the current project configuration will be logged (if any)
     */

    /**
     * Version of the target platform (e.g: 2.0.0.Final)
     * You may instead use streamId to target the latest version of a specific platform stream.
     */
    @Parameter(property = "platformVersion", required = false)
    private String platformVersion;

    /**
     * The OpenRewrite plugin version
     */
    @Parameter(property = "rewritePluginVersion", required = false)
    private String rewritePluginVersion;

    /**
     * Disable the rewrite feature.
     */
    @Parameter(property = "noRewrite", required = false, defaultValue = "false")
    private Boolean noRewrite;

    /**
     * Rewrite in dry-mode.
     */
    @Parameter(property = "rewriteDryRun", required = false, defaultValue = "false")
    private Boolean rewriteDryRun;

    /**
     * The io.quarkus:quarkus-update-recipes version. This artifact contains the base recipes used by this tool to update a
     * project.
     */
    @Parameter(property = "updateRecipesVersion", required = false)
    private String rewriteUpdateRecipesVersion;

    /**
     * Target stream (e.g: 2.0)
     */
    @Parameter(property = "stream", required = false)
    private String stream;

    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession mavenSession;

    @Component
    private BuildPluginManager pluginManager;

    @Override
    protected void validateParameters() throws MojoExecutionException {
        getLog().warn("quarkus:update goal is experimental, its options and output might change in future versions");
        super.validateParameters();
    }

    @Override
    protected void processProjectState(QuarkusProject quarkusProject) throws MojoExecutionException {
        QuarkusProjectHelper.setArtifactResolver(artifactResolver());
        final ExtensionCatalog targetCatalog;
        try {
            if (platformVersion != null) {
                var targetPrimaryBom = getPrimaryBom(quarkusProject.getExtensionsCatalog());
                targetPrimaryBom = ArtifactCoords.pom(targetPrimaryBom.getGroupId(), targetPrimaryBom.getArtifactId(),
                        platformVersion);
                targetCatalog = getExtensionCatalogResolver().resolveExtensionCatalog(List.of(targetPrimaryBom));
            } else if (stream != null) {
                var platformStream = PlatformStreamCoords.fromString(stream);
                targetCatalog = getExtensionCatalogResolver().resolveExtensionCatalog(platformStream);
                platformVersion = getPrimaryBom(targetCatalog).getVersion();
            } else {
                targetCatalog = getExtensionCatalogResolver().resolveExtensionCatalog();
                platformVersion = getPrimaryBom(targetCatalog).getVersion();
            }
        } catch (RegistryResolutionException e) {
            throw new MojoExecutionException(
                    "Failed to resolve the recommended Quarkus extension catalog from the configured extension registries", e);
        }
        final UpdateProject invoker = new UpdateProject(quarkusProject);
        invoker.targetCatalog(targetCatalog);
        invoker.targetPlatformVersion(platformVersion);
        invoker.perModule(perModule);
        invoker.appModel(resolveApplicationModel());
        if (rewritePluginVersion != null) {
            invoker.rewritePluginVersion(rewritePluginVersion);
        }
        if (rewriteUpdateRecipesVersion != null) {
            invoker.rewriteUpdateRecipesVersion(rewriteUpdateRecipesVersion);
        }
        invoker.rewriteDryRun(rewriteDryRun);
        invoker.noRewrite(noRewrite);

        try {
            final QuarkusCommandOutcome result = invoker.execute();
            if (!result.isSuccess()) {
                throw new MojoExecutionException(
                        "The command did not succeed.");
            }
        } catch (QuarkusCommandException e) {
            throw new MojoExecutionException("Failed to resolve the available updates", e);
        }
    }

    private static ArtifactCoords getPrimaryBom(ExtensionCatalog c) {
        return c.getDerivedFrom().isEmpty() ? c.getBom() : c.getDerivedFrom().get(0).getBom();
    }
}
