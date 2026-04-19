package io.quarkus.bootstrap.resolver.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.maven.dependency.DependencyFlags;

/**
 * Tests that when a default capability provider is injected, its conditional
 * dependencies are activated if their conditions are satisfied.
 *
 * <p>
 * Dependency graph:
 * <ul>
 * <li>ext-a (direct dep) -- requires capability "cap.a"</li>
 * <li>ext-b (NOT a project dep) -- provides "cap.a", has conditional dep on ext-c</li>
 * <li>ext-c (conditional) -- condition: ext-a must be present</li>
 * </ul>
 *
 * <p>
 * Platform property: {@code platform.default-capability-provider.cap.a = ext-b coords}
 *
 * <p>
 * Expected outcome: ext-b is injected as a default provider, and ext-c is
 * activated because its condition (ext-a being present) is satisfied.
 */
public class DefaultCapabilityProviderWithConditionalDepsTestCase extends CollectDependenciesBase {

    private TsQuarkusExt extB;

    @Override
    protected void setupDependencies() throws Exception {

        // ext-a is a direct dependency (and serves as the condition for ext-c)
        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");
        extA.setRequiresCapabilities("cap.a");
        install(extA, false);
        root.addDependency(extA);
        addCollectedDep(extA.getRuntime(),
                DependencyFlags.DIRECT
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extA.getDeployment());

        // ext-c is conditional on ext-a
        final TsQuarkusExt extC = new TsQuarkusExt("ext-c");
        extC.setDependencyCondition(extA);
        install(extC, false);

        // ext-b provides "cap.a" and has conditional dep on ext-c
        extB = new TsQuarkusExt("ext-b");
        extB.setProvidesCapabilities("cap.a");
        extB.setConditionalDeps(extC);
        install(extB, false);

        // ext-b should be auto-injected as a direct dependency
        addCollectedDep(extB.getRuntime(),
                DependencyFlags.DIRECT
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extB.getDeployment());

        // ext-c should be activated because ext-a is present
        addCollectedDep(extC.getRuntime(), DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extC.getDeployment());

        installDefaultCapabilityProviders(Map.of("cap.a", extB));
        installPlatformDescriptor();
    }

    @Override
    protected void assertModel(ApplicationModel model) {
        assertThat(model.getAppArtifact().getDependencies())
                .contains(extB.getRuntime().toArtifact());
    }
}
