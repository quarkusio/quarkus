package io.quarkus.bootstrap.resolver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.bootstrap.model.AppDependency;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public abstract class CollectDependenciesBase extends ResolverSetupCleanup {

    protected TsArtifact root;
    protected List<AppDependency> expectedResult = Collections.emptyList();
    protected List<AppDependency> deploymentDeps = Collections.emptyList();

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
        List<AppDependency> expected;
        if (deploymentDeps.isEmpty()) {
            expected = expectedResult;
        } else {
            expected = new ArrayList<>(expectedResult.size() + deploymentDeps.size());
            expected.addAll(expectedResult);
            expected.addAll(deploymentDeps);
        }
        final List<AppDependency> resolvedDeps = getTestResolver().resolveModel(root.toAppArtifact()).getFullDeploymentDeps();
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
            addCollectedDep(ext.getRuntime(), "compile", false, AppDependency.RUNTIME_EXTENSION_ARTIFACT_FLAG);
            addCollectedDeploymentDep(ext.getDeployment());
        }
    }

    protected void installAsDep(TsQuarkusExt ext) {
        ext.install(repo);
        root.addDependency(ext);
        addCollectedDep(ext.getRuntime(), "compile", false, AppDependency.DIRECT_FLAG,
                AppDependency.RUNTIME_EXTENSION_ARTIFACT_FLAG);
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
        int allFlags = AppDependency.DIRECT_FLAG;
        for (int f : flags) {
            allFlags |= f;
        }
        addCollectedDep(artifact, dep.scope == null ? "compile" : dep.scope, dep.optional, allFlags);
    }

    protected void addCollectedDep(final TsArtifact artifact, int... flags) {
        addCollectedDep(artifact, "compile", false, flags);
    }

    protected void addCollectedDep(final TsArtifact artifact, final String scope, boolean optional, int... flags) {
        int allFlags = AppDependency.RUNTIME_CP_FLAG | AppDependency.DEPLOYMENT_CP_FLAG;
        for (int f : flags) {
            allFlags |= f;
        }
        if (expectedResult.isEmpty()) {
            expectedResult = new ArrayList<>();
        }
        expectedResult.add(new AppDependency(artifact.toAppArtifact(), scope, optional, allFlags));
    }

    protected void addCollectedDeploymentDep(TsArtifact ext) {
        if (deploymentDeps.isEmpty()) {
            deploymentDeps = new ArrayList<>();
        }
        deploymentDeps.add(new AppDependency(ext.toAppArtifact(), "compile", false, AppDependency.DEPLOYMENT_CP_FLAG));
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
