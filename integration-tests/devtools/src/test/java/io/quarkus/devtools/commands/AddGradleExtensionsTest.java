package io.quarkus.devtools.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.devtools.project.buildfile.AbstractGroovyGradleBuildFile;
import io.quarkus.devtools.testing.SnapshotTesting;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.RegistryResolutionException;
import io.quarkus.registry.catalog.ExtensionCatalog;

class AddGradleExtensionsTest extends AbstractAddExtensionsTest<List<String>> {

    @Override
    protected List<String> createProject() throws IOException, QuarkusCommandException {
        SnapshotTesting.deleteTestDirectory(getProjectPath().toFile());
        final QuarkusProject project = getQuarkusProject();
        new CreateProject(project)
                .groupId("org.acme")
                .artifactId("add-gradle-extension-test")
                .version("0.0.1-SNAPSHOT")
                .execute();
        return readProject();
    }

    @Override
    protected List<String> readProject() throws IOException {
        return Files.readAllLines(getProjectPath().resolve("build.gradle"));
    }

    @Override
    protected QuarkusCommandOutcome addExtensions(final List<String> extensions) throws IOException, QuarkusCommandException {
        return new AddExtensions(getQuarkusProject())
                .extensions(new HashSet<>(extensions))
                .execute();
    }

    @Override
    protected long countDependencyOccurrences(final List<String> buildFile, final String groupId, final String artifactId,
            final String version) {
        return buildFile.stream()
                .filter(d -> d.equals(getBuildFileDependencyString(groupId, artifactId, version)))
                .count();
    }

    private static String getBuildFileDependencyString(final String groupId, final String artifactId, final String version) {
        final String versionPart = version != null ? ":" + version : "";
        return "    implementation '" + groupId + ":" + artifactId + versionPart + "'";
    }

    protected QuarkusProject getQuarkusProject() throws QuarkusCommandException {
        try {
            return QuarkusProjectHelper.getProject(getProjectPath(),
                    new TestingGradleBuildFile(getProjectPath(), getExtensionsCatalog()));
        } catch (RegistryResolutionException e) {
            throw new QuarkusCommandException("Failed to initialize Quarkus project", e);
        }
    }

    static class TestingGradleBuildFile extends AbstractGroovyGradleBuildFile {

        public TestingGradleBuildFile(Path projectDirPath, ExtensionCatalog catalog) {
            super(projectDirPath, catalog);
        }

        @Override
        protected List<ArtifactCoords> getDependencies() throws IOException {
            final Matcher matcher = Pattern.compile("\\s*implementation\\s+'([^\\v:]+):([^\\v:]+)(:[^:\\v]+)?'")
                    .matcher(getBuildContent());
            final ArrayList<ArtifactCoords> builder = new ArrayList<>();
            while (matcher.find()) {
                builder.add(createDependency(matcher.group(1), matcher.group(2), matcher.group(3), "jar"));
            }
            if (getBuildContent().contains(
                    "implementation enforcedPlatform(\"${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}\")")) {
                builder.add(createDependency(
                        getProperty("quarkusPlatformGroupId"),
                        getProperty("quarkusPlatformArtifactId"),
                        getProperty("quarkusPlatformVersion"),
                        "pom"));
            }
            return builder;
        }

        private ArtifactCoords createDependency(String groupId, String artifactId, String version, String type) {
            return new ArtifactCoords(groupId, artifactId, type, version);
        }

        @Override
        public Collection<ArtifactCoords> getInstalledPlatforms() throws IOException {
            return Collections.emptyList();
        }
    }
}
