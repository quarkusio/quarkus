package io.quarkus.deployment.conditionaldeps;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.deployment.runnerjar.BootstrapFromOriginalJarTestBase;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 * End-to-end deployment test that validates default capability providers
 * are automatically resolved through the full bootstrap process.
 *
 * <p>
 * Dependency graph:
 * <ul>
 * <li>ext-requirer (direct dep) -- requires capability "cap.test"</li>
 * <li>ext-provider (NOT a project dep) -- provides capability "cap.test"</li>
 * </ul>
 *
 * <p>
 * Platform property:
 * {@code platform.default-capability-provider.cap.test = <ext-provider coords>}
 *
 * <p>
 * Expected outcome: ext-provider is automatically injected as a dependency
 * because it is the configured default provider for the required capability
 * "cap.test". Both ext-requirer and ext-provider (and their deployment modules)
 * appear in the resolved {@link ApplicationModel}.
 */
public class DefaultCapabilityProviderEndToEndTest extends BootstrapFromOriginalJarTestBase {

    @Override
    protected TsArtifact composeApplication() {
        final TsQuarkusExt extRequirer = new TsQuarkusExt("ext-requirer");
        extRequirer.setRequiresCapabilities("cap.test");
        install(extRequirer);

        final TsQuarkusExt extProvider = new TsQuarkusExt("ext-provider");
        extProvider.setProvidesCapabilities("cap.test");
        install(extProvider);

        setPlatformProperty(
                BootstrapConstants.DEFAULT_CAPABILITY_PROVIDER_PREFIX + "cap.test",
                TsArtifact.DEFAULT_GROUP_ID + ":ext-provider:" + TsArtifact.DEFAULT_VERSION);

        addToExpectedLib(extRequirer.getRuntime());
        addToExpectedLib(extProvider.getRuntime());

        return TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addDependency(extRequirer);
        // ext-provider is NOT added as an explicit dependency
    }

    @Override
    protected void assertAppModel(ApplicationModel appModel) throws Exception {
        var deps = new HashMap<String, ResolvedDependency>();
        for (var d : appModel.getDependencies()) {
            deps.put(d.getArtifactId(), d);
        }
        assertThat(deps).hasSize(4);

        if (!BootstrapAppModelResolver.isLegacyModelResolver(null)) {
            var extRequirer = deps.get("ext-requirer");
            assertThat(extRequirer.getFlags()).isEqualTo(
                    DependencyFlags.DIRECT
                            | DependencyFlags.RUNTIME_CP
                            | DependencyFlags.DEPLOYMENT_CP
                            | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                            | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
            assertThat(extRequirer.getDependencies()).isEmpty();

            var extRequirerDeployment = deps.get("ext-requirer-deployment");
            assertThat(extRequirerDeployment.getFlags()).isEqualTo(DependencyFlags.DEPLOYMENT_CP);
            assertThat(extRequirerDeployment.getDependencies()).containsExactly(extRequirer);

            var extProvider = deps.get("ext-provider");
            assertThat(extProvider.getFlags()).isEqualTo(
                    DependencyFlags.DIRECT
                            | DependencyFlags.RUNTIME_CP
                            | DependencyFlags.DEPLOYMENT_CP
                            | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                            | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
            assertThat(extProvider.getDependencies()).isEmpty();

            var extProviderDeployment = deps.get("ext-provider-deployment");
            assertThat(extProviderDeployment.getFlags()).isEqualTo(DependencyFlags.DEPLOYMENT_CP);
            assertThat(extProviderDeployment.getDependencies()).containsExactly(extProvider);
        }
    }
}
