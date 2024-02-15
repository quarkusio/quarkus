package io.quarkus.devtools.project.update.rewrite;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.update.ExtensionUpdateInfo;
import io.quarkus.devtools.project.update.ProjectExtensionsUpdateInfo;
import io.quarkus.devtools.project.update.rewrite.QuarkusUpdatesRepository.FetchResult;
import io.quarkus.devtools.project.update.rewrite.operations.DropDependencyVersionOperation;
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
        final FetchResult result = QuarkusUpdatesRepository.fetchRecipes(log, artifactResolver, buildTool,
                updateRecipesVersion,
                request.currentVersion,
                request.targetVersion,
                request.projectExtensionsUpdateInfo
                        .getVersionUpdates());
        QuarkusUpdateRecipe recipe = new QuarkusUpdateRecipe()
                .buildTool(request.buildTool);
        if (request.updateJavaVersion.isPresent()) {
            final String javaVersion = request.updateJavaVersion.get().toString();
            recipe.addOperation(new UpdateJavaVersionOperation(javaVersion));
        }
        switch (request.buildTool) {
            case MAVEN:
                recipe.addOperation(
                        new UpdateDependencyVersionOperation("io.quarkus.platform", "quarkus-bom", request.targetVersion))
                        .addOperation(new UpdateDependencyVersionOperation("io.quarkus", "quarkus-bom", request.targetVersion))
                        .addOperation(new UpdateDependencyVersionOperation("io.quarkus", "quarkus-universe-bom",
                                request.targetVersion))
                        .addOperation(new UpdatePropertyOperation("quarkus.platform.version", request.targetVersion))
                        .addOperation(new UpdatePropertyOperation("quarkus.version", request.targetVersion))
                        .addOperation(new UpdatePropertyOperation("quarkus-plugin.version", request.targetVersion));
                if (request.kotlinVersion != null) {
                    recipe.addOperation(new UpdatePropertyOperation("kotlin.version", request.kotlinVersion));
                }
                break;
            case GRADLE:
            case GRADLE_KOTLIN_DSL:
                recipe.addOperation(new UpdatePropertyOperation("quarkusPlatformVersion", request.targetVersion))
                        .addOperation(new UpdatePropertyOperation("quarkusPluginVersion", request.targetVersion));
                if (request.kotlinVersion != null) {
                    recipe.addOperation(
                            new UpgradeGradlePluginOperation("org.jetbrains.kotlin.*", request.kotlinVersion));
                }
                break;
        }

        for (ExtensionUpdateInfo versionUpdates : request.projectExtensionsUpdateInfo
                .getSimpleVersionUpdates()) {
            if (versionUpdates.getVersionUpdateType()
                    .equals(ExtensionUpdateInfo.VersionUpdateType.RECOMMEND_PLATFORM_MANAGED)) {
                recipe.addOperation(new DropDependencyVersionOperation(
                        versionUpdates.getCurrentDep().getArtifact().getGroupId(),
                        versionUpdates.getCurrentDep().getArtifact().getArtifactId()));
            } else {
                recipe.addOperation(new UpdateDependencyVersionOperation(
                        versionUpdates.getCurrentDep().getArtifact().getGroupId(),
                        versionUpdates.getCurrentDep().getArtifact().getArtifactId(),
                        versionUpdates.getRecommendedDependency().getVersion()));
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
            this(BuildTool.MAVEN, currentVersion, targetVersion, kotlinVersion, updateJavaVersion,
                    projectExtensionsUpdateInfo);
        }

        public ProjectUpdateRequest(BuildTool buildTool, String currentVersion, String targetVersion,
                String kotlinVersion,
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
