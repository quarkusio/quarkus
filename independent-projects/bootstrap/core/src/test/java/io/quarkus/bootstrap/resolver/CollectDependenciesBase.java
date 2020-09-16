package io.quarkus.bootstrap.resolver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.bootstrap.model.AppDependency;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
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
        assertEquals(expected, resolvedDeps);
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

    protected void install(TsQuarkusExt ext) {
        install(ext, true);
    }

    protected void install(TsQuarkusExt ext, boolean collected) {
        ext.install(repo);
        if (collected) {
            addCollectedDep(ext.getRuntime(), "compile", false);
            addCollectedDeploymentDep(ext.getDeployment());
        }
    }

    protected void installAsDep(TsQuarkusExt ext) {
        ext.install(repo);
        root.addDependency(ext);
        addCollectedDep(ext.getRuntime(), "compile", false);
        addCollectedDeploymentDep(ext.getDeployment());
    }

    protected void installAsDep(TsArtifact dep) {
        installAsDep(dep, true);
    }

    protected void installAsDep(TsArtifact dep, boolean collected) {
        installAsDep(dep, null, collected);
    }

    protected void installAsDep(TsArtifact dep, Path p, boolean collected) {
        installAsDep(new TsDependency(dep), p, collected);
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

    protected void installAsDep(TsDependency dep, Path p, boolean collected) {
        final TsArtifact artifact = dep.artifact;
        install(artifact, p);
        root.addDependency(dep);
        if (!collected) {
            return;
        }
        addCollectedDep(artifact, dep.scope == null ? "compile" : dep.scope, dep.optional);
    }

    protected void addCollectedDep(final TsArtifact artifact) {
        addCollectedDep(artifact, "compile", false);
    }

    protected void addCollectedDep(final TsArtifact artifact, final String scope, boolean optional) {
        if (expectedResult.isEmpty()) {
            expectedResult = new ArrayList<>();
        }
        expectedResult.add(new AppDependency(artifact.toAppArtifact(), scope, optional));
    }

    protected void addCollectedDeploymentDep(TsArtifact ext) {
        if (deploymentDeps.isEmpty()) {
            deploymentDeps = new ArrayList<>();
        }
        deploymentDeps.add(new AppDependency(ext.toAppArtifact(), "compile", false));
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
