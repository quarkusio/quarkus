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

    public static void createRecipe(Path target, MavenArtifactResolver artifactResolver, ProjectUpdateRequest request)
            throws IOException {
        final List<String> recipes = QuarkusUpdatesRepository.fetchLatestRecipes(artifactResolver, request.currentVersion,
                request.targetVersion);
        QuarkusUpdateRecipe recipe = new QuarkusUpdateRecipe()
                .buildTool(request.buildTool)
                .addOperation(new UpdatePropertyOperation("quarkus.platform.version", request.targetVersion))
                .addOperation(new UpdatePropertyOperation("quarkus.version", request.targetVersion));
        for (String s : recipes) {
            recipe.addRecipes(QuarkusUpdateRecipeIO.readRecipesYaml(s));
        }
        QuarkusUpdateRecipeIO.write(target, recipe);
    }

    public static class ProjectUpdateRequest {

        public BuildTool buildTool = BuildTool.MAVEN;
        public String currentVersion;
        public String targetVersion;

        public ProjectUpdateRequest(String currentVersion, String targetVersion) {
            this.currentVersion = currentVersion;
            this.targetVersion = targetVersion;
        }
    }
}
