package io.quarkus.devtools.project.buildfile;

import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.maven.ArtifactCoords;
import java.io.IOException;

public final class GroovyGradleBuildFilesCreator extends AbstractGradleBuildFilesCreator {

    public GroovyGradleBuildFilesCreator(QuarkusProject quarkusProject) {
        super(quarkusProject);
    }

    @Override
    String getSettingsGradlePath() {
        return AbstractGroovyGradleBuildFile.SETTINGS_GRADLE_PATH;
    }

    @Override
    String getBuildGradlePath() {
        return AbstractGroovyGradleBuildFile.BUILD_GRADLE_PATH;
    }

    @Override
    void createBuildContent(String groupId, String version) throws IOException {
        final String buildContent = getModel().getBuildContent();
        StringBuilder res = new StringBuilder(buildContent);
        if (!buildContent.contains("id 'io.quarkus'")) {
            res.append("plugins {");
            res.append(System.lineSeparator()).append("    id 'java'").append(System.lineSeparator());
            res.append(System.lineSeparator()).append("    id 'io.quarkus'").append(System.lineSeparator());
            res.append("}");
        }
        final ArtifactCoords bom = getQuarkusProject().getExtensionsCatalog().getBom();
        if (!containsBOM(bom.getGroupId(), bom.getArtifactId())) {
            res.append(System.lineSeparator());
            res.append("dependencies {").append(System.lineSeparator());
            res.append(
                    "    implementation enforcedPlatform(\"${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}\")")
                    .append(System.lineSeparator());
            res.append("    implementation 'io.quarkus:quarkus-resteasy'").append(System.lineSeparator());
            res.append("    testImplementation 'io.quarkus:quarkus-junit5'").append(System.lineSeparator());
            res.append("    testImplementation 'io.rest-assured:rest-assured'").append(System.lineSeparator());
            res.append("}").append(System.lineSeparator());

        }
        String groupLine = "group '" + groupId + "'";
        if (!buildContent.contains(groupLine)) {
            res.append(System.lineSeparator()).append(groupLine)
                    .append(System.lineSeparator());
        }
        String versionLine = "version '" + version + "'";
        if (!buildContent.contains(versionLine)) {
            res.append(System.lineSeparator()).append(versionLine)
                    .append(System.lineSeparator());
        }

        res.append(System.lineSeparator())
                .append("test {").append(System.lineSeparator())
                .append("    systemProperty \"java.util.logging.manager\", \"org.jboss.logmanager.LogManager\"")
                .append(System.lineSeparator())
                .append("}");

        getModel().setBuildContent(res.toString());
    }

    @Override
    void createSettingsContent(String artifactId) throws IOException {
        final String settingsContent = getModel().getSettingsContent();
        final StringBuilder res = new StringBuilder();
        if (!settingsContent.contains("id 'io.quarkus'")) {
            res.append(System.lineSeparator());
            res.append("pluginManagement {").append(System.lineSeparator());
            res.append("    repositories {").append(System.lineSeparator());
            res.append("        mavenLocal()").append(System.lineSeparator());
            res.append("        mavenCentral()").append(System.lineSeparator());
            res.append("        gradlePluginPortal()").append(System.lineSeparator());
            res.append("    }").append(System.lineSeparator());
            res.append("    plugins {").append(System.lineSeparator());
            res.append("        id 'io.quarkus' version \"${quarkusPluginVersion}\"").append(System.lineSeparator());
            res.append("    }").append(System.lineSeparator());
            res.append("}").append(System.lineSeparator());
        }
        if (!settingsContent.contains("rootProject.name")) {
            res.append(System.lineSeparator()).append("rootProject.name='").append(artifactId).append("'")
                    .append(System.lineSeparator());
        }
        res.append(settingsContent);
        getModel().setSettingsContent(res.toString());
    }

    @Override
    void addDependencyInBuildFile(ArtifactCoords coords) throws IOException {
        AbstractGroovyGradleBuildFile.addDependencyInModel(getModel(), coords, false);
    }

}
