package io.quarkus.cli.commands;

import io.quarkus.cli.commands.project.BuildTool;
import io.quarkus.cli.commands.project.QuarkusProject;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Disabled;

class RemoveGradleExtensionsTest extends AbstractRemoveExtensionsTest<List<String>> {

    @Disabled
    void addExtensionTwiceInTwoBatches() throws IOException {
        //FIXME This is currently not working
    }

    @Override
    protected List<String> createProject() throws IOException, QuarkusCommandException {
        CreateProjectTest.delete(getProjectPath().toFile());
        new CreateProject(getProjectPath(), getPlatformDescriptor())
                .buildTool(BuildTool.GRADLE)
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
    protected QuarkusCommandOutcome removeExtensions(final List<String> extensions)
            throws IOException, QuarkusCommandException {
        return new RemoveExtensions(getQuarkusProject())
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

    private QuarkusProject getQuarkusProject() {
        return QuarkusProject.of(getProjectPath(), getPlatformDescriptor(), BuildTool.GRADLE);
    }

    private static String getBuildFileDependencyString(final String groupId, final String artifactId, final String version) {
        final String versionPart = version != null ? ":" + version : "";
        return "    implementation '" + groupId + ":" + artifactId + versionPart + "'";
    }
}
