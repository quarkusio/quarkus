package io.quarkus.deployment.conditionaldeps;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.deployment.runnerjar.BootstrapFromOriginalJarTestBase;
import io.quarkus.maven.dependency.ResolvedDependency;

public class DependencyConditionMatchesConditionalDependencyTest extends BootstrapFromOriginalJarTestBase {

    @Override
    protected TsArtifact composeApplication() {

        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");

        final TsQuarkusExt extB = new TsQuarkusExt("ext-b");
        extB.setDependencyCondition(extA);

        final TsQuarkusExt extD = new TsQuarkusExt("ext-d");
        extD.setDependencyCondition(extB);
        extD.setConditionalDeps(extB);

        final TsQuarkusExt extC = new TsQuarkusExt("ext-c");
        extC.setConditionalDeps(extD);

        install(extA);
        install(extB);
        install(extC);
        install(extD);

        addToExpectedLib(extA.getRuntime());
        addToExpectedLib(extB.getRuntime());
        addToExpectedLib(extC.getRuntime());
        addToExpectedLib(extD.getRuntime());

        return TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addDependency(extC)
                .addDependency(extA);
    }

    @Override
    protected void assertAppModel(ApplicationModel appModel) {
        var extensions = new HashMap<String, ResolvedDependency>();
        for (var d : appModel.getDependencies()) {
            extensions.put(d.getArtifactId(), d);
        }
        assertThat(extensions).hasSize(8);

        if (!BootstrapAppModelResolver.isLegacyModelResolver(null)) {
            var extA = extensions.get("ext-a");
            assertThat(extA.getDependencies()).isEmpty();
            var extADeployment = extensions.get("ext-a-deployment");
            assertThat(extADeployment.getDependencies()).containsExactly(extA);

            var extB = extensions.get("ext-b");
            assertThat(extB.getDependencies()).isEmpty();
            var extBDeployment = extensions.get("ext-b-deployment");
            assertThat(extBDeployment.getDependencies()).containsExactly(extB);

            var extD = extensions.get("ext-d");
            assertThat(extD.getDependencies()).containsExactly(extB);
            var extDDeployment = extensions.get("ext-d-deployment");
            assertThat(extDDeployment.getDependencies()).containsExactlyInAnyOrder(extD, extBDeployment);

            var extC = extensions.get("ext-c");
            assertThat(extC.getDependencies()).containsExactly(extD);
            var extCDeployment = extensions.get("ext-c-deployment");
            assertThat(extCDeployment.getDependencies()).containsExactlyInAnyOrder(extC, extDDeployment);
        }
    }

    @Override
    protected String[] expectedExtensionDependencies() {
        return new String[] {
                "ext-a",
                "ext-b",
                "ext-c",
                "ext-d"
        };
    }
}
