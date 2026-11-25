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
 * <p>
 * In addition, ext-a depends on ext-f, which has a conditional dev dependency on ext-f-dev. That one must not be lost
 * either, even though ext-f is not a direct dependency of the ext-a node that loses version conflict resolution.
 */
public class ConditionalDevDependencyOfExtensionAlsoPulledByConditionalDependencyTestCase extends CollectDependenciesBase {

    @Override
    protected QuarkusBootstrap.Mode getBootstrapMode() {
        return QuarkusBootstrap.Mode.DEV;
    }

    @Override
    protected void setupDependencies() {

        final TsQuarkusExt extF = new TsQuarkusExt("ext-f");
        final TsQuarkusExt extFDev = new TsQuarkusExt("ext-f-dev");
        extFDev.addDependency(extF);
        extF.setConditionalDevDeps(extFDev.getRuntime());

        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");
        extA.addDependency(extF);
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

        installTransitiveExtension(extF);
        installTransitiveExtension(extFDev);
        installTransitiveExtension(extA);
        installTransitiveExtension(extDev);
        installTransitiveExtension(extC);
        installTransitiveExtension(extE);

        // the order matters: ext-b has to be declared before ext-d
        installDirectExtension(extB);
        installDirectExtension(extD);
    }

    private void installTransitiveExtension(TsQuarkusExt ext) {
        install(ext, false);
        addCollectedDep(ext.getRuntime(), DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(ext.getDeployment());
    }

    private void installDirectExtension(TsQuarkusExt ext) {
        install(ext, false);
        installAsDep(ext.getRuntime(),
                DependencyFlags.DIRECT
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(ext.getDeployment());
    }

    @Override
    protected void assertBuildDependencies(Collection<ResolvedDependency> buildDeps) {
        if (BootstrapAppModelResolver.isLegacyModelResolver(null)) {
            return;
        }
        assertDependsOn(buildDeps, "ext-a", "ext-dev-deployment");
        assertDependsOn(buildDeps, "ext-f", "ext-f-dev-deployment");
    }

    private static void assertDependsOn(Collection<ResolvedDependency> buildDeps, String artifactId,
            String dependencyArtifactId) {
        var dep = buildDeps.stream().filter(d -> d.getArtifactId().equals(artifactId)).findFirst().orElseThrow();
        assertThat(dep.getDependencies()).contains(
                ArtifactCoords.jar(TsArtifact.DEFAULT_GROUP_ID, dependencyArtifactId, TsArtifact.DEFAULT_VERSION));
    }
}
