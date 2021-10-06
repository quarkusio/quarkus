package io.quarkus.bootstrap.resolver.update;

import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.resolver.ResolverSetupCleanup;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.bootstrap.util.ZipUtils;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class CreatorOutcomeTestBase extends ResolverSetupCleanup {

    protected TsArtifact appJar;
    protected boolean createWorkspace;

    protected void createWorkspace() {
        this.createWorkspace = true;
    }

    @BeforeEach
    public void initAppModel() throws Exception {
        appJar = modelApp();
        appJar.install(repo);
    }

    @Test
    public void test() throws Exception {
        rebuild();
    }

    protected void rebuild() throws Exception {

        final Path ws = workDir.resolve("workspace");
        IoUtils.recursiveDelete(ws);
        final Path outputDir = IoUtils.mkdirs(ws.resolve("target"));

        Path applicationRoot = resolver.resolve(appJar.toArtifact()).getResolvedPaths().getSinglePath();
        final QuarkusBootstrap.Builder bootstrap = QuarkusBootstrap.builder()
                .setApplicationRoot(applicationRoot)
                .setProjectRoot(applicationRoot)
                .setTargetDirectory(outputDir)
                .setAppModelResolver(resolver);

        if (createWorkspace) {
            System.setProperty("basedir", ws.toAbsolutePath().toString());
            final Path classesDir = outputDir.resolve("classes");
            ZipUtils.unzip(applicationRoot, classesDir);
            ModelUtils.persistModel(ws.resolve("pom.xml"), appJar.getPomModel());
            bootstrap.setProjectRoot(ws);
            bootstrap.setLocalProjectDiscovery(true);
            bootstrap.setAppModelResolver(initResolver(LocalProject.loadWorkspace(classesDir)));
        }

        initProps(bootstrap);
        try {
            testCreator(bootstrap.build());
        } catch (Exception e) {
            assertError(e);
        }

    }

    protected void assertError(Exception e) throws Exception {
        throw e;
    }

    protected abstract TsArtifact modelApp() throws Exception;

    protected abstract void testCreator(QuarkusBootstrap creator) throws Exception;

    protected void initProps(QuarkusBootstrap.Builder builder) {
    }
}
