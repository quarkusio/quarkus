package io.quarkus.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsRepoBuilder;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.maven.dependency.ArtifactCoords;

abstract class DependencyTreeMojoTestBase {
    protected Path workDir;
    protected Path repoHome;

    protected MavenArtifactResolver mvnResolver;
    protected TsRepoBuilder repoBuilder;
    protected TsArtifact app;
    protected Model appModel;

    @BeforeEach
    public void setup() throws Exception {
        workDir = IoUtils.createRandomTmpDir();
        repoHome = IoUtils.mkdirs(workDir.resolve("repo"));

        mvnResolver = MavenArtifactResolver.builder()
                .setOffline(true)
                .setLocalRepository(repoHome.toString())
                .setRemoteRepositories(Collections.emptyList())
                .build();

        repoBuilder = TsRepoBuilder.getInstance(mvnResolver, workDir);
        initRepo();
    }

    @AfterEach
    public void cleanup() {
        if (workDir != null) {
            IoUtils.recursiveDelete(workDir);
        }
    }

    protected abstract void initRepo();

    protected abstract String mode();

    protected boolean isGraph() {
        return false;
    }

    protected boolean isRuntimeOnly() {
        return false;
    }

    @Test
    public void test() throws Exception {

        final DependencyTreeMojo mojo = new DependencyTreeMojo();
        mojo.project = new MavenProject();
        mojo.project.setArtifact(new DefaultArtifact(app.getGroupId(), app.getArtifactId(), app.getVersion(),
                JavaScopes.COMPILE, app.getType(), app.getClassifier(),
                new DefaultArtifactHandler(ArtifactCoords.TYPE_JAR)));
        mojo.project.setModel(appModel);
        mojo.project.setOriginalModel(appModel);
        mojo.resolver = mvnResolver;
        mojo.mode = mode();
        mojo.graph = isGraph();
        mojo.runtimeOnly = isRuntimeOnly();

        final Path mojoLog = workDir.resolve(getClass().getName() + ".log");
        final PrintStream defaultOut = System.out;

        try (PrintStream logOut = new PrintStream(mojoLog.toFile(), StandardCharsets.UTF_8)) {
            System.setOut(logOut);
            mojo.execute();
        } finally {
            System.setOut(defaultOut);
        }

        String expectedFileName = app.getArtifactFileName() + "." + mode();
        if (isRuntimeOnly()) {
            expectedFileName += ".rt";
        }
        assertThat(mojoLog).hasSameTextualContentAs(
                Path.of("").normalize().toAbsolutePath()
                        .resolve("target").resolve("test-classes").resolve(expectedFileName));
    }
}
