package io.quarkus.bootstrap.resolver.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.maven.dependency.DependencyFlags;

/**
 * Tests that when a default capability provider is injected, the managed
 * dependency version from the platform BOM overrides the version specified
 * in the platform property.
 *
 * <p>
 * Setup:
 * <ul>
 * <li>ext-a (direct dep) -- requires capability "cap.a"</li>
 * <li>ext-b version 2 (managed dep) -- provides capability "cap.a"</li>
 * <li>Platform property references ext-b version 1 (older)</li>
 * </ul>
 *
 * <p>
 * Expected outcome: ext-b is injected with version 2 (the managed version),
 * not version 1 (the property version).
 */
public class DefaultCapabilityProviderVersionEnforcementTestCase extends CollectDependenciesBase {

    private TsQuarkusExt extBv2;

    @Override
    protected void setupDependencies() throws Exception {

        // ext-b version 2 is what the managed deps enforce
        extBv2 = new TsQuarkusExt("ext-b", "2");
        extBv2.setProvidesCapabilities("cap.a");
        install(extBv2, false);

        // Also install ext-b version 1 (the property version)
        final TsQuarkusExt extBv1 = new TsQuarkusExt("ext-b", "1");
        extBv1.setProvidesCapabilities("cap.a");
        install(extBv1, false);

        // ext-a requires "cap.a"
        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");
        extA.setRequiresCapabilities("cap.a");
        install(extA, false);
        root.addDependency(extA);
        addCollectedDep(extA.getRuntime(),
                DependencyFlags.DIRECT
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extA.getDeployment());

        // ext-b v2 should be auto-injected as a direct dependency (managed version wins over property version)
        addCollectedDep(extBv2.getRuntime(),
                DependencyFlags.DIRECT
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extBv2.getDeployment());

        // Add ext-b v2 as managed dependency so it overrides the property version
        addManagedDep(extBv2);

        // Property references ext-b version 1, but managed deps should enforce version 2
        installDefaultCapabilityProviders(Map.of("cap.a", extBv1));
        installPlatformDescriptor();
    }

    @Override
    protected void assertModel(ApplicationModel model) {
        assertThat(model.getAppArtifact().getDependencies())
                .contains(extBv2.getRuntime().toArtifact());
    }
}
