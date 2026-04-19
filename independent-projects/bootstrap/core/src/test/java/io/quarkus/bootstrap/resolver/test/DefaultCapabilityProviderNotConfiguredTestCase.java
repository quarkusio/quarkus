package io.quarkus.bootstrap.resolver.test;

import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.maven.dependency.DependencyFlags;

/**
 * Tests that when a direct dependency requires a capability but no default
 * provider is configured, resolution succeeds without error (only a warning is
 * logged) and the provider extension is NOT present in the resolved dependencies.
 *
 * <p>
 * Dependency graph:
 * <ul>
 * <li>ext-a (direct dep) -- requires capability "cap.a"</li>
 * <li>ext-b (NOT a project dep) -- provides capability "cap.a", but no
 * platform property maps "cap.a" to ext-b</li>
 * </ul>
 *
 * <p>
 * No platform properties are installed, so no default provider is configured.
 *
 * <p>
 * Expected outcome: ext-a is resolved normally, ext-b is NOT included in the
 * collected dependencies, and no error is thrown during resolution.
 */
public class DefaultCapabilityProviderNotConfiguredTestCase extends CollectDependenciesBase {

    @Override
    protected void setupDependencies() throws Exception {

        // ext-b provides capability "cap.a" but is NOT a project dependency and
        // NOT configured as a default provider
        final TsQuarkusExt extB = new TsQuarkusExt("ext-b");
        extB.setProvidesCapabilities("cap.a");
        install(extB, false);

        // ext-a requires capability "cap.a" and is a direct project dependency
        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");
        extA.setRequiresCapabilities("cap.a");
        install(extA, false);
        root.addDependency(extA);
        addCollectedDep(extA.getRuntime(),
                DependencyFlags.DIRECT
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extA.getDeployment());

        // ext-b is NOT expected in the result because no default provider is
        // configured for "cap.a"
    }
}
