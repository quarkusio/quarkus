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

public class DevUiStyleConditionalDevModeDependenciesTestCase extends CollectDependenciesBase {

    @Override
    protected QuarkusBootstrap.Mode getBootstrapMode() {
        return QuarkusBootstrap.Mode.DEV;
    }

    @Override
    protected void setupDependencies() {

        final TsArtifact extLibDev = TsArtifact.jar("ext-lib-dev");

        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");
        extA.setConditionalDevDeps(extLibDev);
        extLibDev.addDependency(extA.getRuntime());

        install(extA, false);
        installAsDep(extA.getRuntime(),
                DependencyFlags.DIRECT
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
        install(extLibDev, true);
        addCollectedDeploymentDep(extA.getDeployment());
    }

    @Override
    protected void assertBuildDependencies(Collection<ResolvedDependency> buildDeps) {
        if (BootstrapAppModelResolver.isLegacyModelResolver(null)) {
            return;
        }
        for (var d : buildDeps) {
            switch (d.getArtifactId()) {
                case "ext-a":
                    assertThat(d.getDependencies()).containsExactlyInAnyOrder(
                            ArtifactCoords.jar(TsArtifact.DEFAULT_GROUP_ID, "ext-lib-dev", TsArtifact.DEFAULT_VERSION));
                    break;
                case "ext-a-deployment":
                    assertThat(d.getDependencies()).containsExactlyInAnyOrder(
                            ArtifactCoords.jar(TsArtifact.DEFAULT_GROUP_ID,
                                    d.getArtifactId().substring(0, d.getArtifactId().length() - "-deployment".length()),
                                    TsArtifact.DEFAULT_VERSION));
                    break;
                case "ext-lib-dev":
                    assertThat(d.getDependencies()).containsExactlyInAnyOrder(
                            ArtifactCoords.jar(TsArtifact.DEFAULT_GROUP_ID, "ext-a", TsArtifact.DEFAULT_VERSION));
                    break;
                default:
                    throw new RuntimeException("unexpected dependency " + d.toCompactCoords());
            }
        }
    }
}
