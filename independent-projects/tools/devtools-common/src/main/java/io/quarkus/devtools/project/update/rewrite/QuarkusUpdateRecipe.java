package io.quarkus.devtools.project.update.rewrite;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import io.quarkus.devtools.project.BuildTool;

public class QuarkusUpdateRecipe {

    public static final String RECIPE_IO_QUARKUS_OPENREWRITE_QUARKUS = "io.quarkus.openrewrite.Quarkus";
    public static final Map<String, Object> QUARKUS_RECIPE = Map.of(
            "type", "specs.openrewrite.org/v1beta/recipe",
            "name", RECIPE_IO_QUARKUS_OPENREWRITE_QUARKUS,
            "displayName", "Migrate quarkus project to a new version",
            "description", "Update Quarkus version and refactor imports and resources if needed.",
            "tags", List.of("quarkus"));

    public static final String RECIPE_LIST_KEY = "recipeList";
    private BuildTool buildTool = BuildTool.MAVEN;
    private final List<RewriteOperation> operations = new ArrayList<>();
    private final List<Map<String, Object>> recipes = new ArrayList<>();

    public QuarkusUpdateRecipe() {
    }

    public QuarkusUpdateRecipe buildTool(BuildTool buildTool) {
        this.buildTool = buildTool;
        return this;
    }

    public QuarkusUpdateRecipe addOperation(RewriteOperation operation) {
        this.operations.add(operation);
        return this;
    }

    @SuppressWarnings("unchecked")
    public QuarkusUpdateRecipe addRecipes(List<Object> recipe) {
        for (Object r : recipe) {
            if (r instanceof Map) {
                addRecipe((Map<String, Object>) r);
            }
        }
        return this;
    }

    public QuarkusUpdateRecipe addRecipe(Map<String, Object> recipe) {
        Objects.requireNonNull(recipe, "recipe is required");
        if (!recipe.containsKey("name") || !(recipe.get("name") instanceof String)) {
            throw new IllegalArgumentException("Recipe name is required");
        }
        // some YAML documents might not be recipes. For instance, they could be categories.
        if (!recipe.containsKey("type") || !recipe.get("type").toString().endsWith("/recipe")) {
            return this;
        }
        this.recipes.add(recipe);
        return this;
    }

    public BuildTool getBuildTool() {
        return buildTool;
    }

    public List<RewriteOperation> getOperations() {
        return operations;
    }

    public List<Map<String, Object>> getRecipes() {
        return recipes;
    }

    public List<String> getOtherRecipeNames() {
        return recipes.stream().map(r -> (String) r.get("name")).collect(Collectors.toList());
    }

}
