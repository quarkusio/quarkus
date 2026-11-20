package io.quarkus.bootstrap.resolver.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 * Reproduces the setup in which quarkus-cyclonedx (ext-b) is declared before quarkus-rest (ext-d).
 * quarkus-cyclonedx has a conditional dependency on quarkus-cyclonedx-endpoint (ext-c), which depends on
 * quarkus-vertx-http (ext-a), while quarkus-vertx-http has a conditional dev dependency on quarkus-devui (ext-dev).
 * The conditional dev dependency of ext-a must not be lost when ext-a is also pulled in by an activated
 * conditional dependency.
 */
public class ConditionalDevDependencyOfExtensionAlsoPulledByConditionalDependencyTestCase extends CollectDependenciesBase {

    @Override
    protected QuarkusBootstrap.Mode getBootstrapMode() {
        return QuarkusBootstrap.Mode.DEV;
    }

    @Override
    protected void setupDependencies() {

        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");
        final TsQuarkusExt extDev = new TsQuarkusExt("ext-dev");
        extDev.addDependency(extA);
        extA.setConditionalDevDeps(extDev.getRuntime());

        final TsQuarkusExt extC = new TsQuarkusExt("ext-c");
        extC.addDependency(extA);
        extC.setDependencyCondition(extA);

        final TsQuarkusExt extB = new TsQuarkusExt("ext-b");
        extB.setConditionalDeps(extC);

        final TsQuarkusExt extE = new TsQuarkusExt("ext-e");
        extE.addDependency(extA);

        final TsQuarkusExt extD = new TsQuarkusExt("ext-d");
        extD.addDependency(extE);

        install(extA, false);
        addCollectedDep(extA.getRuntime(), DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extA.getDeployment());

        install(extDev, false);
        addCollectedDep(extDev.getRuntime(), DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extDev.getDeployment());

        install(extC, false);
        addCollectedDep(extC.getRuntime(), DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extC.getDeployment());

        install(extE, false);
        addCollectedDep(extE.getRuntime(), DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extE.getDeployment());

        // the order matters: ext-b has to be declared before ext-d
        install(extB, false);
        installAsDep(extB.getRuntime(),
                DependencyFlags.DIRECT
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extB.getDeployment());

        install(extD, false);
        installAsDep(extD.getRuntime(),
                DependencyFlags.DIRECT
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extD.getDeployment());
    }

    @Override
    protected void assertBuildDependencies(Collection<ResolvedDependency> buildDeps) {
        if (BootstrapAppModelResolver.isLegacyModelResolver(null)) {
            return;
        }
        var extA = buildDeps.stream().filter(d -> d.getArtifactId().equals("ext-a")).findFirst().orElseThrow();
        assertThat(extA.getDependencies()).contains(
                ArtifactCoords.jar(TsArtifact.DEFAULT_GROUP_ID, "ext-dev-deployment", TsArtifact.DEFAULT_VERSION));
    }
}
