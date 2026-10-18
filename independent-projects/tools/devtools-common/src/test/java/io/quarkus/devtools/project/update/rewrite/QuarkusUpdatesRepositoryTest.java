package io.quarkus.devtools.project.update.rewrite;

import static io.quarkus.devtools.project.update.rewrite.QuarkusUpdatesRepository.RecipeDirectory;
import static io.quarkus.devtools.project.update.rewrite.QuarkusUpdatesRepository.fetchUpdateRecipes;
import static io.quarkus.devtools.project.update.rewrite.QuarkusUpdatesRepository.resolveVersionsForRecipesDir;
import static io.quarkus.devtools.project.update.rewrite.QuarkusUpdatesRepository.shouldApplyRecipe;
import static io.quarkus.devtools.project.update.rewrite.QuarkusUpdatesRepository.toKey;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.update.rewrite.QuarkusUpdatesRepository.VersionUpdate;
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
        Map<String, VersionUpdate> recipeDirectoryNames = new LinkedHashMap<>();
        recipeDirectoryNames.put("core", new VersionUpdate("2.7", "3.1"));
        recipeDirectoryNames.put("org.apache.camel.quarkus:camel-quarkus-core", new VersionUpdate("2.7", "3.0"));
        ClassPathResourceLoader resourceLoader = new ClassPathResourceLoader();
        List<String> recipes = fetchUpdateRecipes(MessageWriter.info(), resourceLoader, "dir/quarkus-update",
                recipeDirectoryNames);
        int noOfRecipes = recipes.size();
        assertEquals(3, noOfRecipes);
    }

    @Test
    void testShouldLoadRecipesFromTheDirectoryWithWildcard() throws IOException {
        Map<String, VersionUpdate> recipeDirectoryNames = new LinkedHashMap<>();
        recipeDirectoryNames.put("core", new VersionUpdate("2.7", "3.1"));
        recipeDirectoryNames.put("org.apache.camel.quarkus:camel-quarkus-file", new VersionUpdate("2.7", "3.0"));
        ClassPathResourceLoader resourceLoader = new ClassPathResourceLoader();
        List<String> recipes = fetchUpdateRecipes(MessageWriter.info(), resourceLoader, "dir/quarkus-update",
                recipeDirectoryNames);
        int noOfRecipes = recipes.size();
        assertEquals(3, noOfRecipes);
    }

    @Test
    void testShouldLoadDuplicatedRecipesFromTheDirectoryWithWildcard() throws IOException {
        Map<String, VersionUpdate> recipeDirectoryNames = new LinkedHashMap<>();
        recipeDirectoryNames.put("core", new VersionUpdate("2.7", "3.1"));
        recipeDirectoryNames.put("org.apache.camel.quarkus:camel-quarkus-file", new VersionUpdate("2.7", "3.1"));
        recipeDirectoryNames.put("org.apache.camel.quarkus:camel-quarkus-ftp", new VersionUpdate("2.7", "3.1"));
        recipeDirectoryNames.put("org.apache.camel.quarkus:camel-quarkus-fhir", new VersionUpdate("2.7", "3.1"));
        ClassPathResourceLoader resourceLoader = new ClassPathResourceLoader();
        List<String> recipes = fetchUpdateRecipes(MessageWriter.info(), resourceLoader, "dir/quarkus-update",
                recipeDirectoryNames);
        int noOfRecipes = recipes.size();
        assertEquals(3, noOfRecipes);
    }

    @Test
    void testToKey() {
        String key = toKey("target/classes/quarkus-updates/org.apache.camel.quarkus.camel-quarkus");
        assertEquals("target:classes:quarkus-updates:org.apache.camel.quarkus.camel-quarkus", key);

        key = toKey("../app/target/classes/quarkus-updates/org.apache.camel.quarkus.camel-quarkus");
        assertEquals("..:app:target:classes:quarkus-updates:org.apache.camel.quarkus.camel-quarkus", key);
    }

    @Test
    @EnabledOnOs({ OS.WINDOWS })
    void testToKeyWindows() {
        String key = toKey("..\\a\\b\\quarkus-updates\\org.apache.camel.quarkus.camel-quarkus\\");
        assertEquals("..:a:b:quarkus-updates:org.apache.camel.quarkus.camel-quarkus", key);
    }

    @Test
    void testResolveVersionsForRecipesDir() {
        Map<String, VersionUpdate> recipeDirectoryNames = new LinkedHashMap<>();
        recipeDirectoryNames.put("core", new VersionUpdate("2.7", "3.1"));
        recipeDirectoryNames.put("org.apache.camel.quarkus:camel-something1", new VersionUpdate("2.7", "3.1"));
        recipeDirectoryNames.put("org.apache.camel.quarkus:camel-quarkus-file", new VersionUpdate("2.7", "3.3"));
        recipeDirectoryNames.put("org.apache.camel.quarkus:camel-quarkus-ftp", new VersionUpdate("2.7", "3.4"));
        recipeDirectoryNames.put("org.apache.camel.quarkus:camel-quarkus-fhir", new VersionUpdate("2.7", "3.6"));
        Optional<RecipeDirectory> versions = resolveVersionsForRecipesDir("dir", "org.apache.camel.quarkus:camel-quarkus",
                recipeDirectoryNames);
        assertEquals(3, versions.get().versions().size());

        versions = resolveVersionsForRecipesDir("dir", "org.apache.camel.quarkus:camel", recipeDirectoryNames);
        assertEquals(4, versions.get().versions().size());
    }

}
