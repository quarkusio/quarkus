package io.quarkus.maven;

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
import io.quarkus.devtools.project.update.rewrite.QuarkusUpdateExitErrorException;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.PlatformStreamCoords;

/**
 * Suggest project updates and create a recipe with the possibility to apply it.
 */
@Mojo(name = "update", requiresProject = true)
public class UpdateMojo extends QuarkusProjectStateMojoBase {

    /**
     * Deprecated: this option was unused
     */
    @Parameter(property = "perModule")
    @Deprecated
    boolean perModule;

    /**
     * Version of the target platform (e.g: 2.0.0.Final)
     * You may instead use stream to target the latest version of a specific platform stream.
     */
    @Parameter(property = "platformVersion", required = false)
    private String platformVersion;

    /**
     * The OpenRewrite plugin version
     */
    @Parameter(property = "rewritePluginVersion", required = false)
    private String rewritePluginVersion;

    /**
     * Run the suggested update recipe for this project.
     */
    @Parameter(property = "rewrite", required = false)
    private Boolean rewrite;

    /**
     * Disable the rewrite feature.
     *
     * Deprecated: use -Drewrite=false instead
     */
    @Deprecated
    @Parameter(property = "noRewrite", required = false, defaultValue = "false")
    private Boolean noRewrite;

    /**
     * Do a dry run the suggested update recipe for this project.
     */
    @Parameter(property = "rewriteDryRun", required = false, defaultValue = "false")
    private Boolean rewriteDryRun;

    /**
     * Use custom io.quarkus:quarkus-update-recipes:LATEST coords (GAV) or just provide the version. This artifact should
     * contain the
     * base Quarkus update recipes to update a project.
     */
    @Parameter(property = "quarkusUpdateRecipes", required = false)
    private String rewriteQuarkusUpdateRecipes;

    /**
     * Specify a list of additional artifacts (GAV) containing rewrite recipes
     */
    @Parameter(property = "additionalUpdateRecipes", required = false)
    private String rewriteAdditionalUpdateRecipes;

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
        invoker.appModel(resolveApplicationModel());
        if (rewritePluginVersion != null) {
            invoker.rewritePluginVersion(rewritePluginVersion);
        }
        if (rewriteQuarkusUpdateRecipes != null) {
            invoker.rewriteQuarkusUpdateRecipes(rewriteQuarkusUpdateRecipes);
        }
        if (rewriteAdditionalUpdateRecipes != null) {
            invoker.rewriteAdditionalUpdateRecipes(rewriteAdditionalUpdateRecipes);
        }
        invoker.rewriteDryRun(rewriteDryRun);

        // backward compat
        if (noRewrite != null && noRewrite) {
            rewrite = false;
        }

        if (rewrite != null) {
            invoker.rewrite(rewrite);
        }

        try {
            final QuarkusCommandOutcome result = invoker.execute();
            if (!result.isSuccess()) {
                throw new MojoExecutionException(
                        "Failed to apply the updates: " + result.getMessage());
            }
        } catch (QuarkusUpdateExitErrorException e) {
            throw new MojoExecutionException(e.getMessage());
        } catch (QuarkusCommandException e) {
            throw new MojoExecutionException("Failed to apply the updates", e);
        }
    }

    private static ArtifactCoords getPrimaryBom(ExtensionCatalog c) {
        return c.getDerivedFrom().isEmpty() ? c.getBom() : c.getDerivedFrom().get(0).getBom();
    }
}
