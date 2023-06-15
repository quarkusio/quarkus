package io.quarkus.devtools.project.update.rewrite;

import java.io.IOException;
import java.nio.file.Path;

import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.update.rewrite.QuarkusUpdatesRepository.FetchResult;
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

        for (String s : result.getRecipes()) {
            recipe.addRecipes(QuarkusUpdateRecipeIO.readRecipesYaml(s));
        }
        QuarkusUpdateRecipeIO.write(target, recipe);
        return result;
    }

    public static class ProjectUpdateRequest {

        public BuildTool buildTool;
        public String currentVersion;
        public String targetVersion;
        public String kotlinVersion;

        public ProjectUpdateRequest(String currentVersion, String targetVersion, String kotlinVersion) {
            this(BuildTool.MAVEN, currentVersion, targetVersion, kotlinVersion);
        }

        public ProjectUpdateRequest(BuildTool buildTool, String currentVersion, String targetVersion, String kotlinVersion) {
            this.buildTool = buildTool;
            this.currentVersion = currentVersion;
            this.targetVersion = targetVersion;
            this.kotlinVersion = kotlinVersion;
        }
    }
}
