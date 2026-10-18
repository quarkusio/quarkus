package io.quarkus.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsDependency;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.bootstrap.resolver.TsRepoBuilder;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.maven.dependency.ArtifactCoords;

public class DependencyListMojoTest {

    private Path workDir;
    private MavenArtifactResolver mvnResolver;
    private TsArtifact app;
    private Model appModel;

    @BeforeEach
    public void setup() throws Exception {
        workDir = IoUtils.createRandomTmpDir();
        final Path repoHome = IoUtils.mkdirs(workDir.resolve("repo"));

        mvnResolver = MavenArtifactResolver.builder()
                .setOffline(true)
                .setLocalRepository(repoHome.toString())
                .setRemoteRepositories(Collections.emptyList())
                .build();

        final TsRepoBuilder repoBuilder = TsRepoBuilder.getInstance(mvnResolver, workDir);
        final TsQuarkusExt coreExt = new TsQuarkusExt("test-core-ext");
        app = TsArtifact.jar("test-app")
                .addDependency(new TsArtifact(TsArtifact.DEFAULT_GROUP_ID, "artifact-with-classifier", "classifier", "jar",
                        TsArtifact.DEFAULT_VERSION))
                .addDependency(new TsQuarkusExt("test-ext2")
                        .addDependency(new TsQuarkusExt("test-ext1").addDependency(coreExt)))
                .addDependency(new TsDependency(TsArtifact.jar("optional"), true))
                .addDependency(new TsQuarkusExt("test-ext3").addDependency(coreExt))
                .addDependency(new TsDependency(TsArtifact.jar("provided"), "provided"))
                .addDependency(new TsDependency(TsArtifact.jar("runtime"), "runtime"))
                .addDependency(new TsDependency(TsArtifact.jar("test"), "test"));
        appModel = app.getPomModel();
        app.install(repoBuilder);
    }

    @AfterEach
    public void cleanup() {
        if (workDir != null) {
            IoUtils.recursiveDelete(workDir);
        }
    }

    private DependencyListMojo newMojo(String mode) {
        final DependencyListMojo mojo = new DependencyListMojo();
        mojo.project = new MavenProject();
        mojo.project.setArtifact(new DefaultArtifact(app.getGroupId(), app.getArtifactId(), app.getVersion(),
                JavaScopes.COMPILE, app.getType(), app.getClassifier(),
                new DefaultArtifactHandler(ArtifactCoords.TYPE_JAR)));
        mojo.project.setModel(appModel);
        mojo.project.setOriginalModel(appModel);
        mojo.resolver = mvnResolver;
        mojo.mode = mode;
        return mojo;
    }

    private List<String> execute(DependencyListMojo mojo) throws Exception {
        final Path mojoLog = workDir.resolve("mojo.log");
        final PrintStream defaultOut = System.out;
        try (PrintStream logOut = new PrintStream(mojoLog.toFile(), StandardCharsets.UTF_8)) {
            System.setOut(logOut);
            mojo.execute();
        } finally {
            System.setOut(defaultOut);
        }
        return Files.readAllLines(mojoLog);
    }

    @Test
    public void testProdMode() throws Exception {
        final DependencyListMojo mojo = newMojo("prod");
        final List<String> lines = execute(mojo);

        assertThat(lines.get(0)).contains("Quarkus application PROD mode dependencies:");
        // dependency lines follow the heading
        final List<String> deps = lines.subList(1, lines.size());
        assertThat(deps).isNotEmpty();

        // the list should contain runtime and deployment dependencies
        assertThat(deps).anyMatch(l -> l.contains("test-ext2:"));
        assertThat(deps).anyMatch(l -> l.contains("test-ext2-deployment:"));
        assertThat(deps).anyMatch(l -> l.contains("runtime:"));
        // provided is excluded in prod mode
        assertThat(deps).noneMatch(l -> l.contains("provided:"));

        // verify alphabetical ordering
        final List<String> sorted = new ArrayList<>(deps);
        Collections.sort(sorted);
        assertThat(deps).isEqualTo(sorted);
    }

    @Test
    public void testTestMode() throws Exception {
        final DependencyListMojo mojo = newMojo("test");
        final List<String> lines = execute(mojo);

        assertThat(lines.get(0)).contains("Quarkus application TEST mode dependencies:");
        final List<String> deps = lines.subList(1, lines.size());
        assertThat(deps).isNotEmpty();

        // test mode includes test-scoped dependencies
        assertThat(deps).anyMatch(l -> l.contains("test:"));
    }

    @Test
    public void testFlagsFiltering() throws Exception {
        final DependencyListMojo mojo = newMojo("prod");
        mojo.flags = "RUNTIME_EXTENSION_ARTIFACT";
        final List<String> lines = execute(mojo);

        assertThat(lines.get(0)).contains("with flags RUNTIME_EXTENSION_ARTIFACT");
        final List<String> deps = lines.subList(1, lines.size());
        // only runtime extension artifacts should be listed
        assertThat(deps).allMatch(l -> l.contains("test-ext") || l.contains("test-core-ext"));
        // non-extension deps should not appear
        assertThat(deps).noneMatch(l -> l.contains("artifact-with-classifier"));
        assertThat(deps).noneMatch(l -> l.contains("optional:"));
        assertThat(deps).noneMatch(l -> l.contains("runtime:"));
    }

    @Test
    public void testVerbose() throws Exception {
        final DependencyListMojo mojo = newMojo("prod");
        mojo.verbose = true;
        final List<String> lines = execute(mojo);

        final List<String> deps = lines.subList(1, lines.size());
        assertThat(deps).isNotEmpty();
        // verbose output should include scope and flag names
        for (String dep : deps) {
            assertThat(dep).containsPattern("\\[\\w+\\]");
        }
    }
}
