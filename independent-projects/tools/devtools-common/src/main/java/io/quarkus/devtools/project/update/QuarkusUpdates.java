package io.quarkus.devtools.project.update;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.update.operations.UpdatePropertyOperation;

public final class QuarkusUpdates {

    private QuarkusUpdates() {
    }

    public static void createRecipe(Path target, MavenArtifactResolver artifactResolver, String updateRecipesVersion,
            ProjectUpdateRequest request)
            throws IOException {
        final List<String> recipes = QuarkusUpdatesRepository.fetchRecipes(artifactResolver, updateRecipesVersion,
                request.currentVersion,
                request.targetVersion);
        QuarkusUpdateRecipe recipe = new QuarkusUpdateRecipe()
                .buildTool(request.buildTool);

        switch (request.buildTool) {
            case MAVEN:
                recipe.addOperation(new UpdatePropertyOperation("quarkus.platform.version", request.targetVersion))
                        .addOperation(new UpdatePropertyOperation("quarkus.version", request.targetVersion));
                break;
            case GRADLE:
            case GRADLE_KOTLIN_DSL:
                recipe.addOperation(new UpdatePropertyOperation("quarkusPlatformVersion", request.targetVersion))
                        .addOperation(new UpdatePropertyOperation("quarkusPluginVersion", request.targetVersion));
                break;
        }

        for (String s : recipes) {
            recipe.addRecipes(QuarkusUpdateRecipeIO.readRecipesYaml(s));
        }
        QuarkusUpdateRecipeIO.write(target, recipe);
    }

    public static class ProjectUpdateRequest {

        public BuildTool buildTool;
        public String currentVersion;
        public String targetVersion;

        public ProjectUpdateRequest(String currentVersion, String targetVersion) {
            this(BuildTool.MAVEN, currentVersion, targetVersion);
        }

        public ProjectUpdateRequest(BuildTool buildTool, String currentVersion, String targetVersion) {
            this.buildTool = buildTool;
            this.currentVersion = currentVersion;
            this.targetVersion = targetVersion;
        }
    }
}
