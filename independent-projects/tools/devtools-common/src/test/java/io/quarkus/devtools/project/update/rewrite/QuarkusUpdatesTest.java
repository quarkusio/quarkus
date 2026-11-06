package io.quarkus.devtools.project.update.rewrite;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.update.PlatformInfo;
import io.quarkus.devtools.project.update.ProjectExtensionsUpdateInfo;
import io.quarkus.maven.dependency.ArtifactCoords;

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

    @Test
    void mavenRecipeUpdatesBomsOfEveryImportedPlatform() {
        QuarkusUpdates.ProjectUpdateRequest request = new QuarkusUpdates.ProjectUpdateRequest(BuildTool.MAVEN, "3.20.3",
                "3.27.0", null, null, null, Optional.empty(), new ProjectExtensionsUpdateInfo(Map.of()),
                List.of(new PlatformInfo(ArtifactCoords.pom("io.quarkus.platform", "quarkus-bom", "3.20.3"),
                        ArtifactCoords.pom("io.quarkus.platform", "quarkus-bom", "3.27.0")),
                        new PlatformInfo(ArtifactCoords.pom("org.apache.camel.quarkus", "camel-quarkus-bom", "3.20.0"),
                                ArtifactCoords.pom("org.apache.camel.quarkus", "camel-quarkus-bom", "3.27.0"))));

        String yaml = QuarkusUpdateRecipeIO.toYaml(MessageWriter.info(), QuarkusUpdates.createProjectRecipe(request));

        assertThat(yaml)
                .contains("groupId: io.quarkus.platform")
                .contains("artifactId: quarkus-bom")
                .contains("groupId: org.apache.camel.quarkus")
                .contains("artifactId: camel-quarkus-bom")
                .contains("newVersion: 3.27.0")
                .doesNotContain("newVersion: 3.20");
    }

    @Test
    void gradleRecipeUpdatesImportedPlatformBoms() {
        QuarkusUpdates.ProjectUpdateRequest request = new QuarkusUpdates.ProjectUpdateRequest(BuildTool.GRADLE,
                "3.20.3", "3.27.0", null, null, null, Optional.empty(), new ProjectExtensionsUpdateInfo(Map.of()),
                List.of(new PlatformInfo(
                        ArtifactCoords.pom("org.apache.camel.quarkus", "camel-quarkus-bom", "3.15.0"),
                        ArtifactCoords.pom("org.apache.camel.quarkus", "camel-quarkus-bom", "3.20.0"))));

        String yaml = QuarkusUpdateRecipeIO.toYaml(MessageWriter.info(), QuarkusUpdates.createProjectRecipe(request));

        assertThat(yaml)
                .contains("org.openrewrite.gradle.UpgradeDependencyVersion")
                .contains("groupId: org.apache.camel.quarkus")
                .contains("artifactId: camel-quarkus-bom")
                .contains("newVersion: 3.20.0");
    }
}
