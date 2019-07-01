package io.quarkus.bootstrap.resolver;

import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import io.quarkus.bootstrap.model.AppDependency;

/**
 *
 * @author Alexey Loubyansky
 */
public abstract class CollectDependenciesBase extends ResolverSetupCleanup {

    protected TsArtifact root;
    protected List<AppDependency> expectedResult = Collections.emptyList();

    @Override
    public void setup() throws Exception {
        super.setup();
        root = new TsArtifact("root");
        setupDependencies();
    }

    protected abstract void setupDependencies() throws Exception;

    @Test
    public void testCollectedDependencies() throws Exception {
        install(root);
        final List<AppDependency> resolvedDeps = resolver.resolveModel(root.toAppArtifact()).getAllDependencies();
        assertEquals(expectedResult, resolvedDeps);
    }

    protected TsArtifact install(TsArtifact dep, boolean collected) {
        return install(dep, collected ? "compile" : null);
    }

    protected TsArtifact install(TsArtifact dep, String collectedInScope) {
        return install(dep, null, collectedInScope);
    }

    protected TsArtifact install(TsArtifact dep, Path p, boolean collected) {
        return install(dep, p, collected ? "compile" : null);
    }

    protected TsArtifact install(TsArtifact dep, Path p, String collectedInScope) {
        install(dep, p);
        if(collectedInScope != null) {
            addCollectedDep(dep, collectedInScope, false);
        }
        return dep;
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
        if(!collected) {
            return;
        }
        addCollectedDep(artifact, dep.scope == null ? "compile" : dep.scope, dep.optional);
    }

    protected void addCollectedDep(final TsArtifact artifact) {
        addCollectedDep(artifact, "compile", false);
    }

    protected void addCollectedDep(final TsArtifact artifact, final String scope, boolean optional) {
        if(expectedResult.isEmpty()) {
            expectedResult = new ArrayList<>();
        }
        expectedResult.add(new AppDependency(artifact.toAppArtifact(), scope, optional));
    }

    protected void addManagedDep(TsArtifact dep) {
        root.addManagedDependency(new TsDependency(dep));
    }
}
