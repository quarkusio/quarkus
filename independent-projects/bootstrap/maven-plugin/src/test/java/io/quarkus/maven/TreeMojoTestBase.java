package io.quarkus.maven;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsDependency;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.bootstrap.resolver.TsRepoBuilder;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.util.IoUtils;

public abstract class TreeMojoTestBase {
    protected Path workDir;
    protected Path repoHome;

    protected MavenArtifactResolver mvnResolver;
    protected TsRepoBuilder repoBuilder;
    protected TsArtifact app;
    protected Model appModel;

    @Before
    public void setup() throws Exception {
        workDir = IoUtils.createRandomTmpDir();
        repoHome = IoUtils.mkdirs(workDir.resolve("repo"));

        mvnResolver = MavenArtifactResolver.builder()
                .setOffline(true)
                .setRepoHome(repoHome)
                .setRemoteRepositories(Collections.emptyList())
                .build();

        repoBuilder = TsRepoBuilder.getInstance(new BootstrapAppModelResolver(mvnResolver), workDir);
        initRepo();
    }

    protected void initRepo() throws Exception {
        final TsQuarkusExt coreExt = new TsQuarkusExt("test-core-ext");
        app = TsArtifact.jar("test-app")
                .addDependency(new TsArtifact(TsArtifact.DEFAULT_GROUP_ID, "artifact-with-classifier", "classifier", "jar", TsArtifact.DEFAULT_VERSION))
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

    @After
    public void cleanup() {
        if(workDir != null) {
            IoUtils.recursiveDelete(workDir);
        }
    }

    protected abstract AbstractTreeMojo newTreeMojo();

    protected abstract String mojoName();

    @Test
    public void test() throws Exception {

        final AbstractTreeMojo mojo = newTreeMojo();
        mojo.project = new MavenProject();
        mojo.project.setArtifact(new DefaultArtifact(app.getGroupId(), app.getArtifactId(), app.getVersion(), "compile", app.getType(), app.getClassifier(), new DefaultArtifactHandler("jar")));
        mojo.project.setModel(appModel);
        mojo.project.setOriginalModel(appModel);

        mojo.repoSystem = mvnResolver.getSystem();
        mojo.repoSession = mvnResolver.getSession();
        mojo.repos = mvnResolver.getRepositories();

        final Path mojoLog = workDir.resolve("mojo.log");
        final PrintStream defaultOut = System.out;

        try (PrintStream logOut = new PrintStream(mojoLog.toFile(), "UTF-8")) {
            System.setOut(logOut);
            mojo.execute();
        } finally {
            System.setOut(defaultOut);
        }

        assertEquals(readInLowCase(Paths.get("").toAbsolutePath().resolve("target").resolve("test-classes")
                .resolve(app.getArtifactFileName() + "." + mojoName())), readInLowCase(mojoLog));
    }

    private static List<String> readInLowCase(Path p) throws IOException {
        final List<String> list = new ArrayList<>();
        try(BufferedReader reader = Files.newBufferedReader(p)) {
            String line = reader.readLine();
            while(line != null) {
                list.add(line.toLowerCase());
                line = reader.readLine();
            }
        }
        return list;
    }
}
