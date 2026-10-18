package io.quarkus.devtools.project.update.rewrite;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.update.rewrite.operations.UpdateManagedDependencyVersionOperation;
import io.quarkus.devtools.project.update.rewrite.operations.UpdatePropertyOperation;

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
        final String output = QuarkusUpdateRecipeIO.toYaml(new Log(), recipe);
        System.out.println(output);
        assertThat(output)
                .contains("org.openrewrite.maven.ChangePropertyValue")
                .contains("newValue: 3.0.0.Alpha1")
                .contains("newVersion: example")
                .contains("name: org.openrewrite.java.migrate.JavaxActivationMigrationToJakartaActivation")
                .contains("- org.openrewrite.java.migrate.JavaxActivationMigrationToJakartaActivation")
                .contains("name: org.openrewrite.java.migrate.JavaxXmlSoapToJakartaXmlSoap");
    }

    @Test
    void shoulWarnAboutDuplicatedRecipes() throws IOException {
        final QuarkusUpdateRecipe recipe = new QuarkusUpdateRecipe();

        recipe.addRecipes(QuarkusUpdateRecipeIO
                .readRecipesYaml(
                        new String(QuarkusUpdateRecipeIOTest.class.getResourceAsStream("/duplicated-recipes.yaml")
                                .readAllBytes())));
        Log log = new Log();
        final String output = QuarkusUpdateRecipeIO.toYaml(log, recipe);
        System.out.println(output);
        assertThat(output)
                .contains("org.apache.camel.upgrade.camel411.CamelMigrationRecipe")
                .contains("org.apache.camel.upgrade.camel410.CamelMigrationRecipe");
        assertThat(log.getWarnings())
                .contains("Duplicated recipes found:    - 'io.quarkus.updates.camel.camel410.CamelQuarkusMigrationRecipe'");
    }

    class Log implements MessageWriter {
        private StringBuffer warnings = new StringBuffer();

        @Override
        public void info(String msg) {
        }

        @Override
        public void error(String msg) {
        }

        @Override
        public boolean isDebugEnabled() {
            return false;
        }

        @Override
        public void debug(String msg) {
        }

        @Override
        public void warn(String msg) {
            warnings.append(msg);
        }

        public StringBuffer getWarnings() {
            return warnings;
        }
    };
}
