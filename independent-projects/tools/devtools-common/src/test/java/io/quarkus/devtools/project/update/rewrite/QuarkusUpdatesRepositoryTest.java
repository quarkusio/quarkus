package io.quarkus.devtools.project.update.rewrite;

import static io.quarkus.devtools.project.update.rewrite.QuarkusUpdatesRepository.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
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
    void testShouldLoadRecipesFromTheDirectory() throws IOException  {
        Map<String, String[]> recipeDirectoryNames = new LinkedHashMap<>();
        recipeDirectoryNames.put("core", new String[] {"2.7","3.1"});
        recipeDirectoryNames.put("org.apache.camel.quarkus/camel-quarkus-core", new String[]{"2.7","3.0"});
        ClassPathResourceLoader resourceLoader = new ClassPathResourceLoader();
        List<String> recipes = fetchRecipesAsList(resourceLoader,"dir/quarkus-update", recipeDirectoryNames);
        int noOfRecipes = recipes.size();
        assertEquals(noOfRecipes,3);
        
    }
}




