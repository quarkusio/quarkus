package io.quarkus.devtools.project.update.rewrite;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    public static List<FetchResult> createRecipe(MessageWriter log, Path target, MavenArtifactResolver artifactResolver,
            BuildTool buildTool, String updateRecipesVersion,
            ProjectUpdateRequest request)
            throws IOException {
        List<ExtensionUpdateInfo> extensions = request.projectExtensionsUpdateInfo.getSimpleVersionUpdates();
        List<FetchResult> results = extensions.stream()
        .map(extension -> QuarkusUpdatesRepository.fetchRecipes(log, artifactResolver, buildTool,
                updateRecipesVersion,
                request.currentVersion,
                request.targetVersion,
                extension.getCurrentDep()))
        .collect(Collectors.toList());

        QuarkusUpdateRecipe recipe = new QuarkusUpdateRecipe()
                .buildTool(request.buildTool);
        if (request.updateJavaVersion.isPresent()) {
            final String javaVersion = request.updateJavaVersion.get().toString();
            recipe.addOperation(new UpdateJavaVersionOperation(javaVersion));
        }
        switch (request.buildTool) {
            case MAVEN:
                recipe.addOperation(new UpdatePropertyOperation("quarkus.platform.version", request.targetVersion))
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
            recipe.addOperation(new UpdateDependencyVersionOperation(
                    versionUpdates.getCurrentDep().getArtifact().getGroupId(),
                    versionUpdates.getCurrentDep().getArtifact().getArtifactId(),
                    versionUpdates.getRecommendedDependency().getVersion()));
        }

        results.stream()
                .flatMap(result -> result.getRecipes().stream())
                .map(QuarkusUpdateRecipeIO::readRecipesYaml)
                .forEach(recipe::addRecipes);

        QuarkusUpdateRecipeIO.write(target, recipe);
        return results;
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
