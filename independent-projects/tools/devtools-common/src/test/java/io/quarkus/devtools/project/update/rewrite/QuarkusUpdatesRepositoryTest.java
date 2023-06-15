package io.quarkus.devtools.project.update.rewrite;

import static io.quarkus.devtools.project.update.rewrite.QuarkusUpdatesRepository.shouldApplyRecipe;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

class QuarkusUpdatesRepositoryTest {

    @ParameterizedTest
    @CsvFileSource(resources = "/should_apply_recipe_test_cases.csv", numLinesToSkip = 1)
    void testShouldApplyRecipeWithCSV(String recipeVersion, String currentVersion, String targetVersion,
            boolean expectedResult) {
        boolean result = shouldApplyRecipe(recipeVersion, currentVersion, targetVersion);
        assertEquals(expectedResult, result);
    }

}
