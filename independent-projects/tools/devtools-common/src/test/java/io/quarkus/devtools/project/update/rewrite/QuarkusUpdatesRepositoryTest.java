package io.quarkus.devtools.project.update.rewrite;

import static io.quarkus.devtools.project.update.rewrite.QuarkusUpdatesRepository.applyStartsWith;
import static io.quarkus.devtools.project.update.rewrite.QuarkusUpdatesRepository.fetchUpdateRecipes;
import static io.quarkus.devtools.project.update.rewrite.QuarkusUpdatesRepository.shouldApplyRecipe;
import static io.quarkus.devtools.project.update.rewrite.QuarkusUpdatesRepository.toKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import io.quarkus.platform.descriptor.loader.json.ClassPathResourceLoader;

class QuarkusUpdatesRepositoryTest {

    @ParameterizedTest
    @CsvFileSource(resources = "/should_apply_recipe_test_cases.csv", numLinesToSkip = 1)
    void testShouldApplyRecipeWithCSV(String recipeVersion, String currentVersion, String targetVersion,
            boolean expectedResult) {
        boolean result = shouldApplyRecipe(recipeVersion, currentVersion, targetVersion);
        assertEquals(expectedResult, result);
    }

    @Test
    void testShouldLoadRecipesFromTheDirectory() throws IOException {
        Map<String, String[]> recipeDirectoryNames = new LinkedHashMap<>();
        recipeDirectoryNames.put("core", new String[] { "2.7", "3.1" });
        recipeDirectoryNames.put("org.apache.camel.quarkus:camel-quarkus-core", new String[] { "2.7", "3.0" });
        ClassPathResourceLoader resourceLoader = new ClassPathResourceLoader();
        Map<String, String> recipes = fetchUpdateRecipes(resourceLoader, "dir/quarkus-update", recipeDirectoryNames);
        int noOfRecipes = recipes.size();
        assertEquals(3, noOfRecipes);
    }

    @Test
    void testShouldLoadRecipesFromTheDirectoryWithWildcard() throws IOException {
        Map<String, String[]> recipeDirectoryNames = new LinkedHashMap<>();
        recipeDirectoryNames.put("core", new String[] { "2.7", "3.1" });
        recipeDirectoryNames.put("org.apache.camel.quarkus:camel-quarkus-file", new String[] { "2.7", "3.0" });
        ClassPathResourceLoader resourceLoader = new ClassPathResourceLoader();
        Map<String, String> recipes = fetchUpdateRecipes(resourceLoader, "dir/quarkus-update", recipeDirectoryNames);
        int noOfRecipes = recipes.size();
        assertEquals(3, noOfRecipes);
    }

    @Test
    void testShouldLoadDuplicatedRecipesFromTheDirectoryWithWildcard() throws IOException {
        Map<String, String[]> recipeDirectoryNames = new LinkedHashMap<>();
        recipeDirectoryNames.put("core", new String[] { "2.7", "3.1" });
        recipeDirectoryNames.put("org.apache.camel.quarkus:camel-quarkus-file", new String[] { "2.7", "3.1" });
        recipeDirectoryNames.put("org.apache.camel.quarkus:camel-quarkus-ftp", new String[] { "2.7", "3.1" });
        recipeDirectoryNames.put("org.apache.camel.quarkus:camel-quarkus-fhir", new String[] { "2.7", "3.1" });
        ClassPathResourceLoader resourceLoader = new ClassPathResourceLoader();
        Map<String, String> recipes = fetchUpdateRecipes(resourceLoader, "dir/quarkus-update", recipeDirectoryNames);
        int noOfRecipes = recipes.size();
        assertEquals(3, noOfRecipes);
    }

    @Test
    void testToKey() {
        String key = toKey(Path.of("/home/app"),
                Path.of("/home/app/target/classes/quarkus-updates/org.apache.camel.quarkus.camel-quarkus"));
        assertEquals("target:classes:quarkus-updates:org.apache.camel.quarkus.camel-quarkus", key);

        key = toKey(Path.of("/home/second-app"),
                Path.of("/home/app/target/classes/quarkus-updates/org.apache.camel.quarkus.camel-quarkus"));
        assertEquals("..:app:target:classes:quarkus-updates:org.apache.camel.quarkus.camel-quarkus", key);
    }

    @Test
    @EnabledOnOs({ OS.WINDOWS })
    void testToKeyWindows() {
        String key = toKey(Path.of("C:\\a\\d\\"),
                Path.of("C:\\a\\b\\quarkus-updates\\org.apache.camel.quarkus.camel-quarkus\\"));
        assertEquals("..:b:quarkus-updates:org.apache.camel.quarkus.camel-quarkus", key);
    }

    @Test
    void testApplyStartsWith() {
        Map<String, String[]> recipeDirectoryNames = new LinkedHashMap<>();
        recipeDirectoryNames.put("core", new String[] { "2.7", "3.1" });
        recipeDirectoryNames.put("org.apache.camel.quarkus:camel-something1", new String[] { "2.7", "3.1" });
        recipeDirectoryNames.put("org.apache.camel.quarkus:camel-quarkus-file", new String[] { "2.7", "3.1" });
        recipeDirectoryNames.put("org.apache.camel.quarkus:camel-quarkus-ftp", new String[] { "2.7", "3.1" });
        recipeDirectoryNames.put("org.apache.camel.quarkus:camel-quarkus-fhir", new String[] { "2.7", "3.1" });

        List<String> matchedKeys = applyStartsWith("org.apache.camel.quarkus:camel-quarkus", recipeDirectoryNames);
        assertEquals(3, matchedKeys.size());
        assertTrue(!matchedKeys.contains("org.apache.camel.quarkus:camel-quarkus"));

        matchedKeys = applyStartsWith("org.apache.camel.quarkus:camel", recipeDirectoryNames);
        assertEquals(4, matchedKeys.size());
        assertTrue(!matchedKeys.contains("org.apache.camel.quarkus:camel"));
    }
}
