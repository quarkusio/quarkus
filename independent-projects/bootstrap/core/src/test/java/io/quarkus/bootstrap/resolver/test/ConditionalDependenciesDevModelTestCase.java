package io.quarkus.bootstrap.resolver.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.CollectDependenciesBase;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;

public class ConditionalDependenciesDevModelTestCase extends CollectDependenciesBase {

    @Override
    protected QuarkusBootstrap.Mode getBootstrapMode() {
        return QuarkusBootstrap.Mode.DEV;
    }

    @Override
    protected void setupDependencies() {

        final TsArtifact excludedLib = TsArtifact.jar("excluded-lib");
        install(excludedLib, false);

        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");
        extA.getRuntime().addDependency(excludedLib);
        install(extA, false);
        addCollectedDeploymentDep(extA.getDeployment());

        installAsDep(extA.getRuntime(),
                DependencyFlags.DIRECT
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);

        final TsQuarkusExt extB = new TsQuarkusExt("ext-b");
        extB.setDescriptorProp(ApplicationModelBuilder.EXCLUDED_ARTIFACTS, TsArtifact.DEFAULT_GROUP_ID + ":excluded-lib");
        install(extB, false);
        addCollectedDep(extB.getRuntime(), DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extB.getDeployment());

        final TsQuarkusExt extC = new TsQuarkusExt("ext-c");
        extC.setDependencyCondition(extB);
        install(extC, false);
        addCollectedDep(extC.getRuntime(), DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extC.getDeployment());

        final TsQuarkusExt extD = new TsQuarkusExt("ext-d");
        install(extD, false);
        installAsDep(extD.getRuntime(),
                DependencyFlags.DIRECT
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extD.getDeployment());

        final TsArtifact libE = TsArtifact.jar("lib-e");
        install(libE, true);
        final TsArtifact libEBuildTIme = TsArtifact.jar("lib-e-build-time");
        install(libEBuildTIme);
        addCollectedDeploymentDep(libEBuildTIme);

        final TsQuarkusExt extE = new TsQuarkusExt("ext-e");
        extE.setDependencyCondition(extD);
        extE.getRuntime().addDependency(libE);
        extE.getDeployment().addDependency(libEBuildTIme);
        install(extE, false);
        addCollectedDep(extE.getRuntime(), DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extE.getDeployment());

        final TsQuarkusExt extF = new TsQuarkusExt("ext-f");
        extF.setConditionalDeps(extC, extE);
        install(extF, false);
        installAsDep(extF.getRuntime(),
                DependencyFlags.DIRECT
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extF.getDeployment());

        final TsQuarkusExt extH = new TsQuarkusExt("ext-h");
        install(extH, false);
        addCollectedDep(extH.getRuntime(),
                DependencyFlags.RUNTIME_CP
                        | DependencyFlags.DEPLOYMENT_CP
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extH.getDeployment());
        final TsArtifact devOnlyLib = TsArtifact.jar("dev-only-lib");
        devOnlyLib.addDependency(extH);
        install(devOnlyLib, false);
        addCollectedDep(devOnlyLib, DependencyFlags.RUNTIME_CP | DependencyFlags.DEPLOYMENT_CP);

        final TsQuarkusExt extG = new TsQuarkusExt("ext-g");
        extG.setConditionalDevDeps(extB.getRuntime(), devOnlyLib);
        install(extG, false);
        installAsDep(extG.getRuntime(),
                DependencyFlags.DIRECT
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT
                        | DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
        addCollectedDeploymentDep(extG.getDeployment());
    }

    @Override
    protected void assertBuildDependencies(Collection<ResolvedDependency> buildDeps) {
        if (BootstrapAppModelResolver.isLegacyModelResolver(null)) {
            return;
        }
        for (var d : buildDeps) {
            switch (d.getArtifactId()) {
                case "ext-a":
                case "ext-b":
                case "ext-c":
                case "ext-d":
                case "ext-h":
                case "lib-e":
                case "lib-e-build-time":
                    assertThat(d.getDependencies()).isEmpty();
                    break;
                case "ext-a-deployment":
                case "ext-b-deployment":
                case "ext-c-deployment":
                case "ext-d-deployment":
                case "ext-h-deployment":
                    assertThat(d.getDependencies()).containsExactlyInAnyOrder(
                            ArtifactCoords.jar(TsArtifact.DEFAULT_GROUP_ID,
                                    d.getArtifactId().substring(0, d.getArtifactId().length() - "-deployment".length()),
                                    TsArtifact.DEFAULT_VERSION));
                    break;
                case "ext-e":
                    assertThat(d.getDependencies()).containsExactlyInAnyOrder(
                            ArtifactCoords.jar(TsArtifact.DEFAULT_GROUP_ID, "lib-e", TsArtifact.DEFAULT_VERSION));
                    break;
                case "ext-e-deployment":
                    assertThat(d.getDependencies()).containsExactlyInAnyOrder(
                            ArtifactCoords.jar(TsArtifact.DEFAULT_GROUP_ID, "ext-e", TsArtifact.DEFAULT_VERSION),
                            ArtifactCoords.jar(TsArtifact.DEFAULT_GROUP_ID, "lib-e-build-time", TsArtifact.DEFAULT_VERSION));
                    break;
                case "ext-f":
                    assertThat(d.getDependencies()).containsExactlyInAnyOrder(
                            ArtifactCoords.jar(TsArtifact.DEFAULT_GROUP_ID, "ext-c", TsArtifact.DEFAULT_VERSION),
                            ArtifactCoords.jar(TsArtifact.DEFAULT_GROUP_ID, "ext-e", TsArtifact.DEFAULT_VERSION));
                    break;
                case "ext-f-deployment":
                    assertThat(d.getDependencies()).containsExactlyInAnyOrder(
                            ArtifactCoords.jar(TsArtifact.DEFAULT_GROUP_ID, "ext-f", TsArtifact.DEFAULT_VERSION),
                            ArtifactCoords.jar(TsArtifact.DEFAULT_GROUP_ID, "ext-c-deployment", TsArtifact.DEFAULT_VERSION),
                            ArtifactCoords.jar(TsArtifact.DEFAULT_GROUP_ID, "ext-e-deployment", TsArtifact.DEFAULT_VERSION));
                    break;
                case "ext-g":
                    assertThat(d.getDependencies()).containsExactlyInAnyOrder(
                            ArtifactCoords.jar(TsArtifact.DEFAULT_GROUP_ID, "ext-b", TsArtifact.DEFAULT_VERSION),
                            ArtifactCoords.jar(TsArtifact.DEFAULT_GROUP_ID, "dev-only-lib", TsArtifact.DEFAULT_VERSION));
                    break;
                case "ext-g-deployment":
                    assertThat(d.getDependencies()).containsExactlyInAnyOrder(
                            ArtifactCoords.jar(TsArtifact.DEFAULT_GROUP_ID, "ext-g", TsArtifact.DEFAULT_VERSION),
                            ArtifactCoords.jar(TsArtifact.DEFAULT_GROUP_ID, "ext-b-deployment", TsArtifact.DEFAULT_VERSION));
                    break;
                case "dev-only-lib":
                    assertThat(d.getDependencies()).containsExactlyInAnyOrder(
                            ArtifactCoords.jar(TsArtifact.DEFAULT_GROUP_ID, "ext-h", TsArtifact.DEFAULT_VERSION));
                    break;
                default:
                    throw new RuntimeException("unexpected dependency " + d.toCompactCoords());
            }
        }
    }
}
