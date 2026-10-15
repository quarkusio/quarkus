package io.quarkus.devtools.project.update.rewrite;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.update.ProjectExtensionsUpdateInfo;

class QuarkusUpdatesTest {

    @Test
    void mavenRecipeUpdatesThePluginVersionProperties() {
        QuarkusUpdates.ProjectUpdateRequest request = new QuarkusUpdates.ProjectUpdateRequest(BuildTool.MAVEN, "3.20.3",
                "3.27.0", null, "3.14.0", "3.5.3", Optional.empty(), new ProjectExtensionsUpdateInfo(Map.of()));

        String yaml = QuarkusUpdateRecipeIO.toYaml(MessageWriter.info(), QuarkusUpdates.createProjectRecipe(request));

        assertThat(yaml)
                .contains("key: quarkus.platform.version")
                .contains("key: compiler-plugin.version")
                .contains("newValue: 3.14.0")
                .contains("key: surefire-plugin.version")
                .contains("newValue: 3.5.3");
    }

    @Test
    void pluginVersionPropertiesAreLeftAloneWhenTheCatalogDoesNotProvideThem() {
        QuarkusUpdates.ProjectUpdateRequest request = new QuarkusUpdates.ProjectUpdateRequest(BuildTool.MAVEN, "3.20.3",
                "3.27.0", null, null, null, Optional.empty(), new ProjectExtensionsUpdateInfo(Map.of()));

        String yaml = QuarkusUpdateRecipeIO.toYaml(MessageWriter.info(), QuarkusUpdates.createProjectRecipe(request));

        assertThat(yaml)
                .contains("key: quarkus.platform.version")
                .doesNotContain("compiler-plugin.version")
                .doesNotContain("surefire-plugin.version");
    }
}
