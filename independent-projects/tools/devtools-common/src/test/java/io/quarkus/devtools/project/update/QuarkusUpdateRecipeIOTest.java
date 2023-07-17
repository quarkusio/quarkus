package io.quarkus.devtools.project.update;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.quarkus.devtools.project.update.operations.UpdateManagedDependencyVersionOperation;
import io.quarkus.devtools.project.update.operations.UpdatePropertyOperation;

class QuarkusUpdateRecipeIOTest {

    @Test
    void shouldGenerateYamlCorrectly() throws IOException {
        final QuarkusUpdateRecipe recipe = new QuarkusUpdateRecipe();

        recipe.addRecipes(QuarkusUpdateRecipeIO
                .readRecipesYaml(
                        new String(QuarkusUpdateRecipeIOTest.class.getResourceAsStream("/other-recipes.yaml").readAllBytes())));
        recipe.addOperation(new UpdatePropertyOperation("quarkus.platform.version", "3.0.0.Alpha1"));
        recipe.addOperation(new UpdatePropertyOperation("quarkus.version", "3.0.0.Alpha1"));
        // Just an example
        recipe.addOperation(new UpdateManagedDependencyVersionOperation("io.quarkus", "quarkus-bom", "example"));
        final String output = QuarkusUpdateRecipeIO.toYaml(recipe);
        System.out.println(output);
        assertThat(output)
                .contains("org.openrewrite.maven.ChangePropertyValue")
                .contains("newValue: 3.0.0.Alpha1")
                .contains("newVersion: example")
                .contains("name: org.openrewrite.java.migrate.JavaxActivationMigrationToJakartaActivation")
                .contains("- org.openrewrite.java.migrate.JavaxActivationMigrationToJakartaActivation")
                .contains("name: org.openrewrite.java.migrate.JavaxXmlSoapToJakartaXmlSoap")
                .contains("- org.openrewrite.java.migrate.JavaxXmlSoapToJakartaXmlSoap");
    }
}
