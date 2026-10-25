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
    void mavenRecipeUpdatesNonDefaultImportedPlatformBoms() {
        QuarkusUpdates.ProjectUpdateRequest request = new QuarkusUpdates.ProjectUpdateRequest(BuildTool.MAVEN, "3.20.3",
                "3.27.0", null, null, null, Optional.empty(), new ProjectExtensionsUpdateInfo(Map.of()),
                List.of(new PlatformInfo(ArtifactCoords.pom("io.quarkiverse.platform", "quarkiverse-bom", "1.0.0"),
                        ArtifactCoords.pom("io.quarkiverse.platform", "quarkiverse-bom", "1.2.0"))));

        String yaml = QuarkusUpdateRecipeIO.toYaml(MessageWriter.info(), QuarkusUpdates.createProjectRecipe(request));

        assertThat(yaml)
                .contains("org.openrewrite.maven.UpgradeDependencyVersion")
                .contains("groupId: io.quarkiverse.platform")
                .contains("artifactId: quarkiverse-bom")
                .contains("newVersion: 1.2.0");
    }

    @Test
    void defaultQuarkusPlatformBomsAreNotAddedTwice() {
        QuarkusUpdates.ProjectUpdateRequest request = new QuarkusUpdates.ProjectUpdateRequest(BuildTool.MAVEN, "3.20.3",
                "3.27.0", null, null, null, Optional.empty(), new ProjectExtensionsUpdateInfo(Map.of()),
                List.of(new PlatformInfo(ArtifactCoords.pom("io.quarkus.platform", "quarkus-bom", "3.20.3"),
                        ArtifactCoords.pom("io.quarkus.platform", "quarkus-bom", "3.27.0")),
                        new PlatformInfo(ArtifactCoords.pom("io.quarkus", "quarkus-universe-bom", "2.16.0"),
                                ArtifactCoords.pom("io.quarkus", "quarkus-universe-bom", "3.27.0"))));

        String yaml = QuarkusUpdateRecipeIO.toYaml(MessageWriter.info(), QuarkusUpdates.createProjectRecipe(request));

        assertThat(yaml.split("artifactId: quarkus-bom", -1)).hasSize(3);
        assertThat(yaml.split("artifactId: quarkus-universe-bom", -1)).hasSize(2);
        assertThat(yaml).doesNotContain("quarkiverse-bom");
    }

    @Test
    void gradleRecipeUpdatesNonDefaultImportedPlatformBoms() {
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
