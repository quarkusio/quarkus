package io.quarkus.devtools.project.update.rewrite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import io.quarkus.devtools.messagewriter.MessageWriter;

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
    public static void write(MessageWriter log, Path target, QuarkusUpdateRecipe recipe) throws IOException {
        Objects.requireNonNull(target, "target is required");
        Objects.requireNonNull(recipe, "recipe is required");
        Files.writeString(target, toYaml(log, recipe));
    }

    static String toYaml(MessageWriter log, QuarkusUpdateRecipe recipe) {
        Objects.requireNonNull(recipe, "recipe is required");
        Map<String, Object> q = new HashMap<>();
        q.putAll(QuarkusUpdateRecipe.QUARKUS_RECIPE);
        List<Object> recipeList = new ArrayList<>();
        for (RewriteOperation o : recipe.getOperations()) {
            recipeList.addAll(o.multi(recipe.getBuildTool()));
        }
        recipeList.addAll(recipe.getOtherRecipeNames());
        q.put(QuarkusUpdateRecipe.RECIPE_LIST_KEY, recipeList);
        //warn if there are duplicities among recipes
        Set<Object> seen = new HashSet<>();
        Set<String> duplicates = recipeList.stream()
                .filter(r -> r instanceof String)
                .map(r -> (String) r)
                .filter(r -> !seen.add(r))
                .collect(Collectors.toSet());
        if (!duplicates.isEmpty()) {
            log.warn("Duplicated recipes found:");
            duplicates.stream().sorted().forEach(r -> log.warn("    - '%s'", r));
        }

        List<Object> output = new ArrayList<>();
        output.add(q);
        output.addAll(recipe.getRecipes());
        final DumperOptions options = new DumperOptions();
        options.setPrettyFlow(true);
        Yaml yaml = new Yaml(options);
        return yaml.dumpAll(output.iterator());
    }

}
