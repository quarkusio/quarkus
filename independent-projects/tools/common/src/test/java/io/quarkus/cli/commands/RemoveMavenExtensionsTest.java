package io.quarkus.cli.commands;

import io.quarkus.cli.commands.project.BuildTool;
import io.quarkus.cli.commands.project.QuarkusProject;
import io.quarkus.maven.utilities.MojoUtils;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.apache.maven.model.Model;

class RemoveMavenExtensionsTest extends AbstractRemoveExtensionsTest<Model> {

    @Override
    protected Model createProject() throws IOException, QuarkusCommandException {
        final File pom = getProjectPath().resolve("pom.xml").toFile();
        CreateProjectTest.delete(getProjectPath().toFile());
        new CreateProject(getProjectPath(), getPlatformDescriptor())
                .groupId("org.acme")
                .artifactId("add-maven-extension-test")
                .version("0.0.1-SNAPSHOT")
                .execute();
        return MojoUtils.readPom(pom);
    }

    @Override
    protected Model readProject() throws IOException {
        return MojoUtils.readPom(getProjectPath().resolve("pom.xml").toFile());
    }

    @Override
    protected QuarkusCommandOutcome removeExtensions(List<String> extensions) throws QuarkusCommandException {
        return new RemoveExtensions(getQuarkusProject())
                .extensions(new HashSet<>(extensions))
                .execute();
    }

    @Override
    protected QuarkusCommandOutcome addExtensions(List<String> extensions) throws QuarkusCommandException {
        return new AddExtensions(getQuarkusProject())
                .extensions(new HashSet<>(extensions))
                .execute();
    }

    @Override
    protected long countDependencyOccurrences(final Model project, final String groupId, final String artifactId,
            final String version) {
        return project.getDependencies().stream().filter(d -> d.getGroupId().equals(groupId) &&
                d.getArtifactId().equals(artifactId) &&
                Objects.equals(d.getVersion(), version)).count();
    }

    private QuarkusProject getQuarkusProject() {
        return QuarkusProject.of(getProjectPath(), getPlatformDescriptor(), BuildTool.MAVEN);
    }
}
