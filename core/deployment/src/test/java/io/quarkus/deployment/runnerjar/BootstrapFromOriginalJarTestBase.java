package io.quarkus.deployment.runnerjar;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;

import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.fs.util.ZipUtils;

public abstract class BootstrapFromOriginalJarTestBase extends PackageAppTestBase {

    private TsArtifact appJar;
    private boolean createWorkspace;

    protected void createWorkspace() {
        this.createWorkspace = true;
    }

    @BeforeEach
    public void initAppModel() throws Exception {
        appJar = composeApplication();
        appJar.install(repo);
    }

    protected abstract TsArtifact composeApplication() throws Exception;

    protected QuarkusBootstrap.Builder initBootstrapBuilder()
            throws AppModelResolverException, IOException, Exception, BootstrapMavenException {
        final Path ws = workDir.resolve("workspace");
        IoUtils.recursiveDelete(ws);
        final Path outputDir = IoUtils.mkdirs(ws.resolve("target"));

        Path applicationRoot = resolver.resolve(appJar.toArtifact()).getResolvedPaths().getSinglePath();
        final QuarkusBootstrap.Builder bootstrap = QuarkusBootstrap.builder()
                .setApplicationRoot(applicationRoot)
                .setProjectRoot(applicationRoot)
                .setTargetDirectory(outputDir)
                .setAppModelResolver(resolver)
                .setTest(isBootstrapForTestMode());

        if (createWorkspace) {
            System.setProperty("basedir", ws.toAbsolutePath().toString());
            final Path classesDir = outputDir.resolve("classes");
            ZipUtils.unzip(applicationRoot, classesDir);
            ModelUtils.persistModel(ws.resolve("pom.xml"), appJar.getPomModel());
            bootstrap.setProjectRoot(ws);
            bootstrap.setLocalProjectDiscovery(true);
            bootstrap.setAppModelResolver(initResolver(LocalProject.loadWorkspace(classesDir)));
        }
        return bootstrap;
    }
}
