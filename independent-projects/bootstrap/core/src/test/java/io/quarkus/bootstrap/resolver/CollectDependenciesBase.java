package io.quarkus.bootstrap.resolver;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.aether.util.artifact.JavaScopes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 *
 * @author Alexey Loubyansky
 */
public abstract class CollectDependenciesBase extends ResolverSetupCleanup {

    protected TsArtifact root;
    protected List<Dependency> expectedResult = List.of();
    protected List<Dependency> deploymentDeps = List.of();

    @Override
    @BeforeEach
    public void setup() throws Exception {
        super.setup();
        root = new TsArtifact("root");
        setupDependencies();
    }

    protected abstract void setupDependencies() throws Exception;

    @Test
    public void testCollectedDependencies() throws Exception {
        install(root);
        List<Dependency> expected;
        if (deploymentDeps.isEmpty()) {
            expected = expectedResult;
        } else {
            expected = new ArrayList<>(expectedResult.size() + deploymentDeps.size());
            expected.addAll(expectedResult);
            expected.addAll(deploymentDeps);
        }
        final ApplicationModel model = getTestResolver().resolveModel(root.toArtifact());
        final Collection<ResolvedDependency> buildDeps = model.getDependencies();
        assertThat(stripResolvedPaths(buildDeps)).containsExactlyInAnyOrderElementsOf(expected);
        assertBuildDependencies(buildDeps);
        assertModel(model);
    }

    protected void assertBuildDependencies(Collection<ResolvedDependency> buildDeps) {
    }

    protected void assertModel(ApplicationModel model) {
    }

    private static List<Dependency> stripResolvedPaths(Collection<ResolvedDependency> deps) {
        final List<Dependency> result = new ArrayList<>(deps.size());
        for (var dep : deps) {
            result.add(new ArtifactDependency(dep));
        }
        return result;
    }

    protected BootstrapAppModelResolver getTestResolver() throws Exception {
        return resolver;
    }

    protected Path getInstallDir(TsArtifact artifact) {
        return getInstallDir().resolve(artifact.getGroupId().replace('.', '/')).resolve(artifact.getArtifactId())
                .resolve(artifact.getVersion());
    }

    protected TsArtifact install(TsArtifact dep, boolean collected) {
        return install(dep, collected ? JavaScopes.COMPILE : null);
    }

    protected TsArtifact install(TsArtifact dep, String collectedInScope) {
        return install(dep, null, collectedInScope, false);
    }

    protected TsArtifact install(TsArtifact dep, String collectedInScope, boolean optional) {
        return install(dep, null, collectedInScope, optional);
    }

    protected TsArtifact install(TsArtifact dep, Path p, boolean collected) {
        return install(dep, p, collected ? JavaScopes.COMPILE : null, false);
    }

    protected TsArtifact install(TsArtifact dep, Path p, String collectedInScope, boolean optional) {
        install(dep, p);
        if (collectedInScope != null) {
            addCollectedDep(dep, collectedInScope, optional);
        }
        return dep;
    }

    protected TsQuarkusExt install(TsQuarkusExt ext) {
        install(ext, true);
        return ext;
    }

    protected void install(TsQuarkusExt ext, boolean collected) {
        ext.install(repo);
        if (collected) {
            addCollectedDep(ext.getRuntime(), JavaScopes.COMPILE, false, DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
            addCollectedDeploymentDep(ext.getDeployment());
        }
    }

    protected void installAsDep(TsQuarkusExt ext) {
        ext.install(repo);
        root.addDependency(ext);
        addCollectedDep(ext.getRuntime(), JavaScopes.COMPILE, false,
                DependencyFlags.DIRECT | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(ext.getDeployment());
    }

    protected void installAsDep(TsArtifact dep, int... flags) {
        installAsDep(dep, true, flags);
    }

    protected void installAsDep(TsArtifact dep, boolean collected, int... flags) {
        installAsDep(dep, null, collected, flags);
    }

    protected void installAsDep(TsArtifact dep, Path p, boolean collected, int... flags) {
        installAsDep(new TsDependency(dep), p, collected, flags);
    }

    protected void installAsDep(TsDependency dep) {
        installAsDep(dep, null);
    }

    protected void installAsDep(TsDependency dep, Path p) {
        installAsDep(dep, p, true);
    }

    protected void installAsDep(TsDependency dep, boolean collected) {
        installAsDep(dep, null, collected);
    }

    protected void installAsDep(TsDependency dep, Path p, boolean collected, int... flags) {
        final TsArtifact artifact = dep.artifact;
        install(artifact, p);
        root.addDependency(dep);
        if (!collected) {
            return;
        }
        int allFlags = DependencyFlags.DIRECT;
        for (int f : flags) {
            allFlags |= f;
        }
        addCollectedDep(artifact, dep.scope == null ? JavaScopes.COMPILE : dep.scope, dep.optional, allFlags);
    }

    protected void addCollectedDep(final TsArtifact artifact, int... flags) {
        addCollectedDep(artifact, JavaScopes.COMPILE, false, flags);
    }

    protected void addCollectedDep(final TsArtifact artifact, final String scope, boolean optional, int... flags) {
        int allFlags = DependencyFlags.RUNTIME_CP | DependencyFlags.DEPLOYMENT_CP;
        if (optional) {
            allFlags |= DependencyFlags.OPTIONAL;
        }
        for (int f : flags) {
            allFlags |= f;
        }
        if (expectedResult.isEmpty()) {
            expectedResult = new ArrayList<>();
        }
        expectedResult.add(new ArtifactDependency(artifact.toArtifact(), scope, allFlags));
    }

    protected void addCollectedDeploymentDep(TsArtifact ext) {
        if (deploymentDeps.isEmpty()) {
            deploymentDeps = new ArrayList<>();
        }
        deploymentDeps
                .add(new ArtifactDependency(ext.toArtifact(), JavaScopes.COMPILE,
                        DependencyFlags.DEPLOYMENT_CP));
    }

    protected void addManagedDep(TsQuarkusExt ext) {
        addManagedDep(ext.runtime);
        addManagedDep(ext.deployment);
    }

    protected void addManagedDep(TsArtifact dep) {
        root.addManagedDependency(new TsDependency(dep));
    }

    protected void addDep(TsArtifact dep) {
        root.addDependency(dep);
    }

    protected void setPomProperty(String name, String value) {
        root.setPomProperty(name, value);
    }

    protected void installDefaultCapabilityProviders(Map<String, TsQuarkusExt> capabilityProviders) {
        final TsArtifact platformProps = new TsArtifact(TsArtifact.DEFAULT_GROUP_ID,
                "test" + BootstrapConstants.PLATFORM_PROPERTIES_ARTIFACT_ID_SUFFIX,
                null, "properties", TsArtifact.DEFAULT_VERSION);
        platformProps.setContent(new TsArtifact.ContentProvider() {
            Path propsFile;

            @Override
            public Path getPath(Path workDir) throws IOException {
                if (propsFile == null) {
                    final Properties props = new Properties();
                    for (var entry : capabilityProviders.entrySet()) {
                        final TsArtifact rt = entry.getValue().getRuntime();
                        props.setProperty(
                                BootstrapConstants.DEFAULT_CAPABILITY_PROVIDER_PREFIX + entry.getKey(),
                                rt.getGroupId() + ":" + rt.getArtifactId() + ":" + rt.getVersion());
                    }
                    propsFile = workDir.resolve("platform-properties.properties");
                    try (OutputStream os = Files.newOutputStream(propsFile)) {
                        props.store(os, "Test platform properties");
                    }
                }
                return propsFile;
            }
        });
        platformProps.install(repo);
        addManagedDep(platformProps);
    }

    protected void installPlatformDescriptor() {
        final TsArtifact platformDescr = new TsArtifact(TsArtifact.DEFAULT_GROUP_ID,
                "test" + BootstrapConstants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX,
                "1.0", "json", TsArtifact.DEFAULT_VERSION);
        platformDescr.setContent(new TsArtifact.ContentProvider() {
            Path json;

            @Override
            public Path getPath(Path workDir) throws IOException {
                if (json == null) {
                    json = workDir.resolve("platform-descriptor.json");
                    Files.writeString(json, "platform descriptor");
                }
                return json;
            }
        });
        platformDescr.install(repo);
        addManagedDep(platformDescr);
    }
}
