package io.quarkus.bootstrap.resolver;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.util.IoUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
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

    protected Map<String, String> originalProps;

    @BeforeEach
    public void setup() throws Exception {
        setSystemProperties();
        workDir = initWorkDir();
        repoHome = IoUtils.mkdirs(workDir.resolve("repo"));
        resolver = newAppModelResolver(null);
        repo = TsRepoBuilder.getInstance(resolver, workDir);
    }

    @AfterEach
    public void cleanup() {
        if (cleanWorkDir() && workDir != null) {
            IoUtils.recursiveDelete(workDir);
        }
        if (originalProps != null) {
            for (Map.Entry<String, String> prop : originalProps.entrySet()) {
                if (prop.getValue() == null) {
                    System.clearProperty(prop.getKey());
                } else {
                    System.setProperty(prop.getKey(), prop.getValue());
                }
            }
            originalProps = null;
        }
    }

    protected void setSystemProperties() {
    }

    protected void setSystemProperty(String name, String value) {
        if (originalProps == null) {
            originalProps = new HashMap<>();
        }
        final String prevValue = System.setProperty(name, value);
        if (!originalProps.containsKey(name)) {
            originalProps.put(name, prevValue);
        }
    }

    protected Path initWorkDir() {
        return IoUtils.createRandomTmpDir();
    }

    protected boolean cleanWorkDir() {
        return true;
    }

    protected boolean isBootstrapForTestMode() {
        return false;
    }

    protected BootstrapAppModelResolver newAppModelResolver(LocalProject currentProject) throws Exception {
        final BootstrapAppModelResolver appModelResolver = new BootstrapAppModelResolver(newArtifactResolver(currentProject));
        if (isBootstrapForTestMode()) {
            appModelResolver.setTest(true);
        }
        return appModelResolver;
    }

    protected MavenArtifactResolver newArtifactResolver(LocalProject currentProject) throws BootstrapMavenException {
        return MavenArtifactResolver.builder()
                .setLocalRepository(repoHome.toString())
                .setOffline(true)
                .setWorkspaceDiscovery(false)
                .setCurrentProject(currentProject)
                .build();
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
