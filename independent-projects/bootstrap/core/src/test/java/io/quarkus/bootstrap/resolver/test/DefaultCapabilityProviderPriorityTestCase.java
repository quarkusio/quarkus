package io.quarkus.bootstrap.resolver.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.maven.dependency.DependencyFlags;

/**
 * Tests that default capability provider resolution respects priority ordering:
 * when two unsatisfied capabilities both have default providers, the one
 * required by the shallowest extension is resolved first.
 *
 * <p>
 * Dependency graph:
 * <ul>
 * <li>ext-a (direct dep) -- requires capability "cap.a"</li>
 * <li>ext-b (direct dep) -- depends on ext-c (transitive)</li>
 * <li>ext-c (transitive, depth 2) -- requires capability "cap.c"</li>
 * <li>ext-provider-a (NOT a project dep) -- provides "cap.a"</li>
 * <li>ext-provider-c (NOT a project dep) -- provides "cap.c"</li>
 * </ul>
 *
 * <p>
 * Platform properties configure default providers for both capabilities.
 *
 * <p>
 * Expected outcome: both providers are injected. ext-a's requirement (depth 1)
 * is resolved before ext-c's requirement (depth 2), but both end up in the
 * final dependency set regardless of order.
 */
public class DefaultCapabilityProviderPriorityTestCase extends CollectDependenciesBase {

    private TsQuarkusExt extProviderA;
    private TsQuarkusExt extProviderC;

    @Override
    protected void setupDependencies() throws Exception {

        // ext-provider-a provides "cap.a"
        extProviderA = new TsQuarkusExt("ext-provider-a");
        extProviderA.setProvidesCapabilities("cap.a");
        install(extProviderA, false);

        // ext-provider-c provides "cap.c"
        extProviderC = new TsQuarkusExt("ext-provider-c");
        extProviderC.setProvidesCapabilities("cap.c");
        install(extProviderC, false);

        // ext-c requires "cap.c", will be transitive via ext-b
        final TsQuarkusExt extC = new TsQuarkusExt("ext-c");
        extC.setRequiresCapabilities("cap.c");
        install(extC, false);

        // ext-b depends on ext-c
        final TsQuarkusExt extB = new TsQuarkusExt("ext-b");
        extB.addDependency(extC);
        install(extB, false);
        root.addDependency(extB);
        addCollectedDep(extB.getRuntime(),
                DependencyFlags.DIRECT
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extB.getDeployment());

        // ext-c is transitive
        addCollectedDep(extC.getRuntime(), DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extC.getDeployment());

        // ext-a requires "cap.a", direct dep (shallower than ext-c)
        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");
        extA.setRequiresCapabilities("cap.a");
        install(extA, false);
        root.addDependency(extA);
        addCollectedDep(extA.getRuntime(),
                DependencyFlags.DIRECT
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extA.getDeployment());

        // both providers should be auto-injected as direct dependencies
        addCollectedDep(extProviderA.getRuntime(),
                DependencyFlags.DIRECT
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extProviderA.getDeployment());
        addCollectedDep(extProviderC.getRuntime(),
                DependencyFlags.DIRECT
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extProviderC.getDeployment());

        installDefaultCapabilityProviders(Map.of("cap.a", extProviderA, "cap.c", extProviderC));
        installPlatformDescriptor();
    }

    @Override
    protected void assertModel(ApplicationModel model) {
        assertThat(model.getAppArtifact().getDependencies())
                .contains(extProviderA.getRuntime().toArtifact(), extProviderC.getRuntime().toArtifact());
    }
}
