package io.quarkus.devtools.commands;

import com.google.common.collect.ImmutableList;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.buildfile.AbstractGradleBuildFile;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.model.Dependency;

class AddGradleExtensionsTest extends AbstractAddExtensionsTest<List<String>> {

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

    private QuarkusProject getQuarkusProject() {
        final Path projectPath = getProjectPath();
        final QuarkusPlatformDescriptor platformDescriptor = getPlatformDescriptor();
        return QuarkusProject.of(projectPath, platformDescriptor, new TestingGradleBuildFile(projectPath, platformDescriptor));
    }

    static class TestingGradleBuildFile extends AbstractGradleBuildFile {

        public TestingGradleBuildFile(Path projectFolderPath, QuarkusPlatformDescriptor platformDescriptor) {
            super(projectFolderPath, platformDescriptor);
        }

        @Override
        protected List<Dependency> getDependencies() throws IOException {
            final Matcher matcher = Pattern.compile("\\s*implementation\\s+'([^\\v:]+):([^\\v:]+)(:[^:\\v]+)?'")
                    .matcher(getBuildContent());
            final ImmutableList.Builder<Dependency> builder = ImmutableList.builder();
            while (matcher.find()) {
                final Dependency dep = new Dependency();
                dep.setGroupId(matcher.group(1));
                dep.setArtifactId(matcher.group(2));
                dep.setVersion(matcher.group(3));
                builder.add(dep);
            }
            return builder.build();
        }
    }
}
