package io.quarkus.bootstrap.resolver.test;

import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.util.IoUtils;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 */
public class SystemPropertyOverridesPomPropertyDependencyTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() {

        final TsArtifact x12 = new TsArtifact("x", "2");
        final TsArtifact x13 = new TsArtifact("x", "3");

        install(x12);
        install(x13);

        // x.version in pom is 2
        setPomProperty("x.version", "2");
        addDep(new TsArtifact("x", "${x.version}"));

        // the system property of x.version is 3
        setSystemProperty("x.version", "3");

        // it is expected that the system property will dominate
        addCollectedDep(x13, AppDependency.DIRECT_FLAG);
    }

    @Override
    protected BootstrapAppModelResolver getTestResolver() throws Exception {

        // location of the root in the local maven repo
        final Path installDir = getInstallDir(root);

        // here i'm faking a project from which the root could have been installed
        final Path projectDir = workDir.resolve("project");
        Files.createDirectories(projectDir);
        IoUtils.copy(installDir.resolve(root.toPomArtifact().getArtifactFileName()), projectDir.resolve("pom.xml"));

        // workspace reader for the root project
        final LocalProject currentProject = LocalProject.loadWorkspace(projectDir);

        return initResolver(currentProject);
    }
}
