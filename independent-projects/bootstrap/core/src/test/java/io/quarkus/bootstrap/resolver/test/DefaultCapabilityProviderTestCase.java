package io.quarkus.bootstrap.resolver.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.maven.dependency.DependencyFlags;

/**
 * Tests that a default capability provider is automatically resolved when a
 * direct dependency requires a capability that no present extension provides.
 *
 * <p>
 * Dependency graph:
 * <ul>
 * <li>ext-a (direct dep) -- requires capability "cap.a"</li>
 * <li>ext-b (NOT a project dep) -- provides capability "cap.a"</li>
 * </ul>
 *
 * <p>
 * Platform property: {@code platform.default-capability-provider.cap.a = <ext-b coords>}
 *
 * <p>
 * Expected outcome: ext-b is automatically injected as a dependency because it
 * is the configured default provider for the required capability "cap.a".
 */
public class DefaultCapabilityProviderTestCase extends CollectDependenciesBase {

    private TsQuarkusExt extB;

    @Override
    protected void setupDependencies() throws Exception {

        // ext-b provides capability "cap.a" but is NOT a direct project dependency
        extB = new TsQuarkusExt("ext-b");
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

        // ext-b should be auto-injected as a direct dependency
        addCollectedDep(extB.getRuntime(),
                DependencyFlags.DIRECT
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extB.getDeployment());

        installDefaultCapabilityProviders(Map.of("cap.a", extB));
        installPlatformDescriptor();
    }

    @Override
    protected void assertModel(ApplicationModel model) {
        assertThat(model.getAppArtifact().getDependencies())
                .as("Injected default provider should appear in app artifact dependencies")
                .contains(extB.getRuntime().toArtifact());
    }
}
