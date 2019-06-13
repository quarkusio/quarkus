package io.quarkus.bootstrap.resolver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.util.IoUtils;
import org.junit.After;
import org.junit.Before;

/**
 *
 * @author Alexey Loubyansky
 */
public class ResolverSetupCleanup {

    protected Path workDir;
    protected Path repoHome;
    protected BootstrapAppModelResolver resolver;
    protected TsRepoBuilder repo;

    @Before
    public void setup() throws Exception {
        workDir = IoUtils.createRandomTmpDir();
        repoHome = IoUtils.mkdirs(workDir.resolve("repo"));
        resolver = initResolver();
        repo = TsRepoBuilder.getInstance(resolver, workDir);
    }

    @After
    public void cleanup() {
        if(workDir != null) {
            IoUtils.recursiveDelete(workDir);
        }
    }

    protected BootstrapAppModelResolver initResolver() throws AppModelResolverException {
        return new BootstrapAppModelResolver(MavenArtifactResolver.builder()
                .setRepoHome(repoHome)
                .setOffline(true)
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
