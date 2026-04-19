package io.quarkus.bootstrap.resolver.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.maven.dependency.DependencyFlags;

/**
 * Tests chained default capability provider resolution: an injected default
 * provider itself requires another capability whose default provider must also
 * be injected.
 *
 * <p>
 * Dependency graph:
 * <ul>
 * <li>ext-a (direct dep) -- requires capability "cap.a"</li>
 * <li>ext-b (NOT a project dep) -- provides "cap.a", requires "cap.b"</li>
 * <li>ext-c (NOT a project dep) -- provides "cap.b"</li>
 * </ul>
 *
 * <p>
 * Platform properties:
 * <ul>
 * <li>{@code platform.default-capability-provider.cap.a = ext-b coords}</li>
 * <li>{@code platform.default-capability-provider.cap.b = ext-c coords}</li>
 * </ul>
 *
 * <p>
 * Expected outcome: both ext-b and ext-c are automatically injected into the
 * resolved dependency set. ext-b satisfies ext-a's requirement for "cap.a",
 * and ext-c satisfies ext-b's requirement for "cap.b".
 */
public class DefaultCapabilityProviderChainedTestCase extends CollectDependenciesBase {

    private TsQuarkusExt extB;
    private TsQuarkusExt extC;

    @Override
    protected void setupDependencies() throws Exception {

        // ext-c provides "cap.b", not a project dep
        extC = new TsQuarkusExt("ext-c");
        extC.setProvidesCapabilities("cap.b");
        install(extC, false);

        // ext-b provides "cap.a" and requires "cap.b", not a project dep
        extB = new TsQuarkusExt("ext-b");
        extB.setProvidesCapabilities("cap.a");
        extB.setRequiresCapabilities("cap.b");
        install(extB, false);

        // ext-a requires "cap.a", is a direct project dependency
        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");
        extA.setRequiresCapabilities("cap.a");
        install(extA, false);
        root.addDependency(extA);
        addCollectedDep(extA.getRuntime(),
                DependencyFlags.DIRECT
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extA.getDeployment());

        // ext-b should be auto-injected as a direct dependency (provides cap.a)
        addCollectedDep(extB.getRuntime(),
                DependencyFlags.DIRECT
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extB.getDeployment());

        // ext-c should be auto-injected as a direct dependency (provides cap.b, required by ext-b)
        addCollectedDep(extC.getRuntime(),
                DependencyFlags.DIRECT
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extC.getDeployment());

        installDefaultCapabilityProviders(Map.of("cap.a", extB, "cap.b", extC));
        installPlatformDescriptor();
    }

    @Override
    protected void assertModel(ApplicationModel model) {
        assertThat(model.getAppArtifact().getDependencies())
                .contains(extB.getRuntime().toArtifact(), extC.getRuntime().toArtifact());
    }
}
