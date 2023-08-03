package io.quarkus.devtools.project.update.rewrite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class QuarkusUpdateRecipeIO {

    /**
     * Read recipe to be used with {@link QuarkusUpdateRecipe#addRecipes(List)}
     *
     * @param recipeYaml the recipe yaml
     * @return
     */
    public static List<Object> readRecipesYaml(String recipeYaml) {
        Objects.requireNonNull(recipeYaml, "inputStream is required");
        Yaml yaml = new Yaml();
        List<Object> otherRecipes = new ArrayList<>();
        yaml.loadAll(recipeYaml).iterator().forEachRemaining(otherRecipes::add);
        return List.copyOf(otherRecipes);
    }

    /**
     * Write a Quarkus project update recipe on the disk
     *
     * @param target
     * @param recipe
     * @throws IOException
     */
    public static void write(Path target, QuarkusUpdateRecipe recipe) throws IOException {
        Objects.requireNonNull(target, "target is required");
        Objects.requireNonNull(recipe, "recipe is required");
        Files.writeString(target, toYaml(recipe));
    }

    static String toYaml(QuarkusUpdateRecipe recipe) {
        Objects.requireNonNull(recipe, "recipe is required");
        Map<String, Object> q = new HashMap<>();
        q.putAll(QuarkusUpdateRecipe.QUARKUS_RECIPE);
        List<Object> recipeList = new ArrayList<>();
        for (RewriteOperation o : recipe.getOperations()) {
            recipeList.addAll(o.multi(recipe.getBuildTool()));
        }
        recipeList.addAll(recipe.getOtherRecipeNames());
        q.put(QuarkusUpdateRecipe.RECIPE_LIST_KEY, recipeList);
        List<Object> output = new ArrayList<>();
        output.add(q);
        output.addAll(recipe.getRecipes());
        final DumperOptions options = new DumperOptions();
        options.setPrettyFlow(true);
        Yaml yaml = new Yaml(options);
        return yaml.dumpAll(output.iterator());
    }

}
