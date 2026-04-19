package io.quarkus.bootstrap.resolver.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.maven.dependency.DependencyFlags;

/**
 * Tests that when multiple extensions require the same capability, the default
 * provider is injected only once.
 *
 * <p>
 * Dependency graph:
 * <ul>
 * <li>ext-a (direct dep) -- requires capability "cap.a"</li>
 * <li>ext-d (direct dep) -- depends on ext-c (transitive)</li>
 * <li>ext-c (transitive dep of ext-d) -- requires capability "cap.a"</li>
 * <li>ext-b (NOT a project dep) -- provides capability "cap.a"</li>
 * </ul>
 *
 * <p>
 * Platform property: {@code platform.default-capability-provider.cap.a = ext-b coords}
 *
 * <p>
 * Expected outcome: ext-b is injected exactly once as a dependency, satisfying
 * the "cap.a" requirement from both ext-a (direct) and ext-c (transitive via ext-d).
 */
public class DefaultCapabilityProviderMultipleRequirersTestCase extends CollectDependenciesBase {

    private TsQuarkusExt extB;

    @Override
    protected void setupDependencies() throws Exception {

        // ext-b provides "cap.a", not a project dep
        extB = new TsQuarkusExt("ext-b");
        extB.setProvidesCapabilities("cap.a");
        install(extB, false);

        // ext-c requires "cap.a", will be a transitive dep via ext-d
        final TsQuarkusExt extC = new TsQuarkusExt("ext-c");
        extC.setRequiresCapabilities("cap.a");
        install(extC, false);

        // ext-d depends on ext-c, is a direct project dependency
        final TsQuarkusExt extD = new TsQuarkusExt("ext-d");
        extD.addDependency(extC);
        install(extD, false);
        root.addDependency(extD);
        addCollectedDep(extD.getRuntime(),
                DependencyFlags.DIRECT
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extD.getDeployment());

        // ext-c is a transitive dependency via ext-d
        addCollectedDep(extC.getRuntime(), DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extC.getDeployment());

        // ext-a requires "cap.a", direct project dependency
        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");
        extA.setRequiresCapabilities("cap.a");
        install(extA, false);
        root.addDependency(extA);
        addCollectedDep(extA.getRuntime(),
                DependencyFlags.DIRECT
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extA.getDeployment());

        // ext-b should be auto-injected once as a direct dependency
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
                .contains(extB.getRuntime().toArtifact());
    }
}
