package io.quarkus.devtools.commands;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import org.apache.maven.model.Model;

import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.QuarkusProject;
import io.quarkus.devtools.project.QuarkusProjectHelper;
import io.quarkus.devtools.testing.SnapshotTesting;
import io.quarkus.maven.utilities.MojoUtils;

class RemoveMavenExtensionsTest extends AbstractRemoveExtensionsTest<Model> {

    @Override
    protected Model createProject() throws IOException, QuarkusCommandException {
        final File pom = getProjectPath().resolve("pom.xml").toFile();
        SnapshotTesting.deleteTestDirectory(getProjectPath().toFile());
        final QuarkusProject project = getQuarkusProject();
        new CreateProject(project)
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
        return QuarkusProjectHelper.getProject(getProjectPath(), BuildTool.MAVEN);
    }
}
