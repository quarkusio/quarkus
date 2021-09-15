package io.quarkus.bootstrap.resolver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedArtifactDependency;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public abstract class CollectDependenciesBase extends ResolverSetupCleanup {

    protected TsArtifact root;
    protected List<Dependency> expectedResult = Collections.emptyList();
    protected List<Dependency> deploymentDeps = Collections.emptyList();

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
        // stripping the resolved paths
        final List<Dependency> resolvedDeps = getTestResolver().resolveModel(root.toArtifact()).getDependencies()
                .stream()
                .map(d -> new ArtifactDependency(d)).collect(Collectors.toList());
        assertEquals(new HashSet<>(expected), new HashSet<>(resolvedDeps));
    }

    protected BootstrapAppModelResolver getTestResolver() throws Exception {
        return resolver;
    }

    protected Path getInstallDir(TsArtifact artifact) {
        return repoHome.resolve(artifact.getGroupId().replace('.', '/')).resolve(artifact.getArtifactId())
                .resolve(artifact.getVersion());
    }

    protected TsArtifact install(TsArtifact dep, boolean collected) {
        return install(dep, collected ? "compile" : null);
    }

    protected TsArtifact install(TsArtifact dep, String collectedInScope) {
        return install(dep, null, collectedInScope, false);
    }

    protected TsArtifact install(TsArtifact dep, String collectedInScope, boolean optional) {
        return install(dep, null, collectedInScope, optional);
    }

    protected TsArtifact install(TsArtifact dep, Path p, boolean collected) {
        return install(dep, p, collected ? "compile" : null, false);
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
            addCollectedDep(ext.getRuntime(), "compile", false, DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
            addCollectedDeploymentDep(ext.getDeployment());
        }
    }

    protected void installAsDep(TsQuarkusExt ext) {
        ext.install(repo);
        root.addDependency(ext);
        addCollectedDep(ext.getRuntime(), "compile", false,
                DependencyFlags.DIRECT | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
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
        addCollectedDep(artifact, dep.scope == null ? "compile" : dep.scope, dep.optional, allFlags);
    }

    protected void addCollectedDep(final TsArtifact artifact, int... flags) {
        addCollectedDep(artifact, "compile", false, flags);
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
        expectedResult.add(new ArtifactDependency(new ResolvedArtifactDependency(artifact.toArtifact()), scope, allFlags));
    }

    protected void addCollectedDeploymentDep(TsArtifact ext) {
        if (deploymentDeps.isEmpty()) {
            deploymentDeps = new ArrayList<>();
        }
        deploymentDeps
                .add(new ArtifactDependency(new ResolvedArtifactDependency(ext.toArtifact()), "compile",
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
}
