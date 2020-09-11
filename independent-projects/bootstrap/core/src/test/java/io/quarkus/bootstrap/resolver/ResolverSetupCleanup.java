package io.quarkus.bootstrap.resolver;

import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.util.IoUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 *
 * @author Alexey Loubyansky
 */
public class ResolverSetupCleanup {

    protected Path workDir;
    protected Path repoHome;
    protected BootstrapAppModelResolver resolver;
    protected TsRepoBuilder repo;

    protected Properties originalProps;

    @BeforeEach
    public void setup() throws Exception {
        workDir = initWorkDir();
        repoHome = IoUtils.mkdirs(workDir.resolve("repo"));
        resolver = initResolver(null);
        repo = TsRepoBuilder.getInstance(resolver, workDir);
    }

    @AfterEach
    public void cleanup() {
        if (cleanWorkDir() && workDir != null) {
            IoUtils.recursiveDelete(workDir);
        }
        if (originalProps != null) {
            System.setProperties(originalProps);
        }
    }

    protected void setSystemProperty(String name, String value) {
        if (originalProps == null) {
            originalProps = new Properties(System.getProperties());
        }
        System.setProperty(name, value);
    }

    protected Path initWorkDir() {
        return IoUtils.createRandomTmpDir();
    }

    protected boolean cleanWorkDir() {
        return true;
    }

    protected BootstrapAppModelResolver initResolver(LocalProject currentProject) throws Exception {
        return new BootstrapAppModelResolver(MavenArtifactResolver.builder()
                .setLocalRepository(repoHome.toString())
                .setOffline(true)
                .setWorkspaceDiscovery(false)
                .setCurrentProject(currentProject)
                .build());
    }

    protected TsJar newJar() throws IOException {
        return new TsJar(workDir.resolve(UUID.randomUUID().toString()));
    }

    protected TsQuarkusExt install(TsQuarkusExt extension) {
        extension.install(repo);
        return extension;
    }

    protected TsArtifact install(TsArtifact artifact) {
        repo.install(artifact);
        return artifact;
    }

    protected TsArtifact install(TsArtifact artifact, Path p) {
        repo.install(artifact, p);
        return artifact;
    }
}
