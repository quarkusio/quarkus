package io.quarkus.bootstrap.resolver.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.maven.dependency.DependencyFlags;

/**
 * Tests that a default capability provider injection triggers activation of
 * previously unsatisfied conditional dependencies on existing extensions.
 *
 * <p>
 * Dependency graph:
 * <ul>
 * <li>ext-a (direct dep) -- requires capability "cap.a", has conditional dep ext-c</li>
 * <li>ext-b (NOT a project dep) -- provides "cap.a"</li>
 * <li>ext-c (conditional) -- condition: ext-b must be present</li>
 * </ul>
 *
 * <p>
 * Platform property: {@code platform.default-capability-provider.cap.a = ext-b coords}
 *
 * <p>
 * Expected outcome: ext-b is injected as a default provider, then ext-c is
 * activated because its condition (ext-b being present) is now satisfied.
 * This mirrors the spring-web / quarkus-rest-jackson / spring-web-rest scenario.
 */
public class DefaultCapabilityProviderActivatesConditionalDepTestCase extends CollectDependenciesBase {

    private TsQuarkusExt extB;

    @Override
    protected void setupDependencies() throws Exception {

        // ext-b provides capability "cap.a" but is NOT a direct project dependency
        extB = new TsQuarkusExt("ext-b");
        extB.setProvidesCapabilities("cap.a");
        install(extB, false);

        // ext-c is conditional on ext-b (the default provider)
        final TsQuarkusExt extC = new TsQuarkusExt("ext-c");
        extC.setDependencyCondition(extB);
        install(extC, false);

        // ext-a requires capability "cap.a" and has a conditional dep on ext-c
        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");
        extA.setRequiresCapabilities("cap.a");
        extA.setConditionalDeps(extC);
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

        // ext-c should be activated because ext-b (the default provider) is now present
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
