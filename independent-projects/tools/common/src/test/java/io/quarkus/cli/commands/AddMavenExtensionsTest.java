package io.quarkus.cli.commands;

import io.quarkus.cli.commands.writer.FileProjectWriter;
import io.quarkus.maven.utilities.MojoUtils;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.apache.maven.model.Model;

class AddMavenExtensionsTest extends AbstractAddExtensionsTest<Model> {

    @Override
    protected Model createProject() throws IOException, QuarkusCommandException {
        final File pom = getProjectPath().resolve("pom.xml").toFile();
        CreateProjectTest.delete(getProjectPath().toFile());
        new CreateProject(new FileProjectWriter(getProjectPath().toFile()), getPlatformDescriptor())
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
    protected QuarkusCommandOutcome addExtensions(List<String> extensions) throws IOException, QuarkusCommandException {
        return new AddExtensions(new FileProjectWriter(getProjectPath().toFile()), getPlatformDescriptor())
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
}
