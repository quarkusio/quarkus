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

public abstract class QuarkusUpdate extends QuarkusPlatformTask {

    private Boolean noRewrite;
    private Boolean rewrite;

    private boolean rewriteDryRun;

    private String targetStreamId;
    private String targetPlatformVersion;

    private String rewritePluginVersion = null;

    private String rewriteQuarkusUpdateRecipes = null;
    private String rewriteAdditionalUpdateRecipes = null;

    @Input
    @Optional
    @Deprecated
    public Boolean getNoRewrite() {
        return noRewrite;
    }

    @Option(description = "Disable the rewrite feature (deprecated use --rewrite=false instead).", option = "noRewrite")
    @Deprecated
    public QuarkusUpdate setNoRewrite(Boolean noRewrite) {
        this.noRewrite = noRewrite;
        return this;
    }

    @Input
    @Optional
    public Boolean getRewrite() {
        return rewrite;
    }

    @Option(description = "Run the suggested update recipe for this project.", option = "rewrite")
    public QuarkusUpdate setRewrite(Boolean rewrite) {
        this.rewrite = rewrite;
        return this;
    }

    @Input
    @Optional
    public Boolean getRewriteDryRun() {
        return rewriteDryRun;
    }

    @Option(description = "Do a dry run of the suggested update recipe for this project.", option = "rewriteDryRun")
    public QuarkusUpdate setRewriteDryRun(Boolean rewriteDryRun) {
        this.rewriteDryRun = rewriteDryRun;
        return this;
    }

    @Input
    public boolean getPerModule() {
        return false;
    }

    @Option(description = "This option was not used", option = "perModule")
    @Deprecated
    public void setPerModule(boolean perModule) {
    }

    @Input
    @Optional
    public String getRewritePluginVersion() {
        return rewritePluginVersion;
    }

    @Option(description = "The OpenRewrite plugin version", option = "rewritePluginVersion")
    public void setRewritePluginVersion(String rewritePluginVersion) {
        this.rewritePluginVersion = rewritePluginVersion;
    }

    @Input
    @Optional
    public String getRewriteQuarkusUpdateRecipes() {
        return rewriteQuarkusUpdateRecipes;
    }

    @Option(description = "Use a custom io.quarkus:quarkus-update-recipes:LATEST artifact (GAV) or just provide the version. This artifact should contain the base Quarkus update recipes to update a project.", option = "quarkusUpdateRecipes")
    public QuarkusUpdate setRewriteQuarkusUpdateRecipes(String rewriteQuarkusUpdateRecipes) {
        this.rewriteQuarkusUpdateRecipes = rewriteQuarkusUpdateRecipes;
        return this;
    }

    @Input
    @Optional
    public String getRewriteAdditionalUpdateRecipes() {
        return rewriteAdditionalUpdateRecipes;
    }

    @Option(description = "Specify a list of additional artifacts (GAV) containing rewrite recipes.", option = "additionalUpdateRecipes")
    public QuarkusUpdate setRewriteAdditionalUpdateRecipes(String rewriteAdditionalUpdateRecipes) {
        this.rewriteAdditionalUpdateRecipes = rewriteAdditionalUpdateRecipes;
        return this;
    }

    @Input
    @Optional
    public String getTargetStreamId() {
        return targetStreamId;
    }

    @Option(description = "A target stream, for example:  2.0", option = "stream")
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
        getLogger().warn(getName() + " is experimental, its options and output might change in future versions");
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
        invoker.targetCatalog(targetCatalog);
        if (rewriteQuarkusUpdateRecipes != null) {
            invoker.rewriteQuarkusUpdateRecipes(rewriteQuarkusUpdateRecipes);
        }
        if (rewriteAdditionalUpdateRecipes != null) {
            invoker.rewriteAdditionalUpdateRecipes(rewriteAdditionalUpdateRecipes);
        }
        if (rewritePluginVersion != null) {
            invoker.rewritePluginVersion(rewritePluginVersion);
        }
        invoker.targetPlatformVersion(targetPlatformVersion);
        invoker.rewriteDryRun(rewriteDryRun);

        // backward compat
        if (noRewrite != null && noRewrite) {
            rewrite = false;
        }

        if (rewrite != null) {
            invoker.rewrite(rewrite);
        }
        invoker.appModel(extension().getApplicationModel());
        try {
            invoker.execute();
        } catch (Exception e) {
            throw new GradleException("Failed to apply recommended updates", e);
        }
    }

    private static ArtifactCoords getPrimaryBom(ExtensionCatalog c) {
        return c.getDerivedFrom().isEmpty() ? c.getBom() : c.getDerivedFrom().get(0).getBom();
    }
}
