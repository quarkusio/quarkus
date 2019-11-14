package io.quarkus.bootstrap.resolver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalWorkspace;
import io.quarkus.bootstrap.util.IoUtils;
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

    @BeforeEach
    public void setup() throws Exception {
        workDir = initWorkDir();
        repoHome = IoUtils.mkdirs(workDir.resolve("repo"));
        resolver = initResolver(null);
        repo = TsRepoBuilder.getInstance(resolver, workDir);
    }

    @AfterEach
    public void cleanup() {
        if(cleanWorkDir() && workDir != null) {
            IoUtils.recursiveDelete(workDir);
        }
    }

    protected Path initWorkDir() {
        return IoUtils.createRandomTmpDir();
    }

    protected boolean cleanWorkDir() {
        return true;
    }

    protected BootstrapAppModelResolver initResolver(LocalWorkspace workspace) throws AppModelResolverException {
        return new BootstrapAppModelResolver(MavenArtifactResolver.builder()
                .setRepoHome(repoHome)
                .setOffline(true)
                .setWorkspace(workspace)
                .build());
    }

    protected TsJar newJar() throws IOException {
        return new TsJar(workDir.resolve(UUID.randomUUID().toString()));
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
