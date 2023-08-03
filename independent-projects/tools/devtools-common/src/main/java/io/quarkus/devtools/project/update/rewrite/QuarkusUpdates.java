package io.quarkus.devtools.project.update.rewrite;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.update.ExtensionUpdateInfo;
import io.quarkus.devtools.project.update.ProjectExtensionsUpdateInfo;
import io.quarkus.devtools.project.update.rewrite.QuarkusUpdatesRepository.FetchResult;
import io.quarkus.devtools.project.update.rewrite.operations.UpdateDependencyVersionOperation;
import io.quarkus.devtools.project.update.rewrite.operations.UpdateJavaVersionOperation;
import io.quarkus.devtools.project.update.rewrite.operations.UpdatePropertyOperation;
import io.quarkus.devtools.project.update.rewrite.operations.UpgradeGradlePluginOperation;

public final class QuarkusUpdates {

    private QuarkusUpdates() {
    }

    public static FetchResult createRecipe(MessageWriter log, Path target, MavenArtifactResolver artifactResolver,
            BuildTool buildTool, String updateRecipesVersion,
            ProjectUpdateRequest request)
            throws IOException {
        final FetchResult result = QuarkusUpdatesRepository.fetchRecipes(log, artifactResolver, buildTool, updateRecipesVersion,
                request.currentVersion,
                request.targetVersion);
        QuarkusUpdateRecipe recipe = new QuarkusUpdateRecipe()
                .buildTool(request.buildTool);
        if (request.updateJavaVersion.isPresent()) {
            final String javaVersion = request.updateJavaVersion.get().toString();
            recipe.addOperation(new UpdateJavaVersionOperation(javaVersion));
        }
        switch (request.buildTool) {
            case MAVEN:
                recipe.addOperation(new UpdatePropertyOperation("quarkus.platform.version", request.targetVersion))
                        .addOperation(new UpdatePropertyOperation("quarkus.version", request.targetVersion));
                if (request.kotlinVersion != null) {
                    recipe.addOperation(new UpdatePropertyOperation("kotlin.version", request.kotlinVersion));
                }
                break;
            case GRADLE:
            case GRADLE_KOTLIN_DSL:
                recipe.addOperation(new UpdatePropertyOperation("quarkusPlatformVersion", request.targetVersion))
                        .addOperation(new UpdatePropertyOperation("quarkusPluginVersion", request.targetVersion));
                if (request.kotlinVersion != null) {
                    recipe.addOperation(new UpgradeGradlePluginOperation("org.jetbrains.kotlin.*", request.kotlinVersion));
                }
                break;
        }

        for (List<ExtensionUpdateInfo> nonPlatformExtensionsUpdates : request.projectExtensionsUpdateInfo
                .getNonPlatformExtensions().values()) {
            for (ExtensionUpdateInfo nonPlatformExtensionsUpdate : nonPlatformExtensionsUpdates) {
                if (nonPlatformExtensionsUpdate.getCurrentDep().isPlatformExtension()) {
                    // add, my understanding is that we should define the version? As a dependency, as a managed one?
                    // not completely sure how to make it work for a multi-module project?
                } else if (nonPlatformExtensionsUpdate.getRecommendedDependency().isPlatformExtension()) {
                    // remove, decide what to do here, should we remove the version given it is now managed? Will OpenRewrite support that?
                    // not completely sure how to make it work for a multi-module project?
                } else {
                    recipe.addOperation(new UpdateDependencyVersionOperation(
                            nonPlatformExtensionsUpdate.getCurrentDep().getArtifact().getGroupId(),
                            nonPlatformExtensionsUpdate.getCurrentDep().getArtifact().getArtifactId(),
                            nonPlatformExtensionsUpdate.getRecommendedDependency().getVersion()));
                }
            }
        }

        for (String s : result.getRecipes()) {
            recipe.addRecipes(QuarkusUpdateRecipeIO.readRecipesYaml(s));
        }
        QuarkusUpdateRecipeIO.write(target, recipe);
        return result;
    }

    public static class ProjectUpdateRequest {

        public final BuildTool buildTool;
        public final String currentVersion;
        public final String targetVersion;
        public final String kotlinVersion;
        public final Optional<Integer> updateJavaVersion;
        public final ProjectExtensionsUpdateInfo projectExtensionsUpdateInfo;

        public ProjectUpdateRequest(String currentVersion, String targetVersion, String kotlinVersion,
                Optional<Integer> updateJavaVersion, ProjectExtensionsUpdateInfo projectExtensionsUpdateInfo) {
            this(BuildTool.MAVEN, currentVersion, targetVersion, kotlinVersion, updateJavaVersion, projectExtensionsUpdateInfo);
        }

        public ProjectUpdateRequest(BuildTool buildTool, String currentVersion, String targetVersion, String kotlinVersion,
                Optional<Integer> updateJavaVersion, ProjectExtensionsUpdateInfo projectExtensionsUpdateInfo) {
            this.buildTool = buildTool;
            this.currentVersion = currentVersion;
            this.targetVersion = targetVersion;
            this.kotlinVersion = kotlinVersion;
            this.updateJavaVersion = updateJavaVersion;
            this.projectExtensionsUpdateInfo = projectExtensionsUpdateInfo;
        }
    }
}
