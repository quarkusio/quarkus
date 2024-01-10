package io.quarkus.deployment.runnerjar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.aether.util.artifact.JavaScopes;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsDependency;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.DependencyFlags;

public class ProvidedExtensionDepsTestModeTest extends BootstrapFromOriginalJarTestBase {

    @Override
    protected boolean isBootstrapForTestMode() {
        return true;
    }

    @Override
    protected TsArtifact composeApplication() {

        final TsArtifact extADep = TsArtifact.jar("ext-a-dep");
        addToExpectedLib(extADep);

        final TsArtifact depC1 = TsArtifact.jar("dep-c");
        //addToExpectedLib(depC1);
        extADep.addDependency(depC1);

        final TsArtifact extAProvidedDep = TsArtifact.jar("ext-a-provided-dep");

        final TsArtifact extADeploymentDep = TsArtifact.jar("ext-a-deployment-dep");
        final TsArtifact extAOptionalDeploymentDep = TsArtifact.jar("ext-a-provided-deployment-dep");

        final TsQuarkusExt extA = new TsQuarkusExt("ext-a");
        addToExpectedLib(extA.getRuntime());
        extA.getRuntime()
                .addDependency(extADep)
                .addDependency(new TsDependency(extAProvidedDep, JavaScopes.PROVIDED));
        extA.getDeployment()
                .addDependency(extADeploymentDep)
                .addDependency(new TsDependency(extAOptionalDeploymentDep, JavaScopes.PROVIDED));

        final TsQuarkusExt extB = new TsQuarkusExt("ext-b");
        addToExpectedLib(extB.getRuntime());
        this.install(extB);

        final TsArtifact directProvidedDep = TsArtifact.jar("direct-provided-dep");
        addToExpectedLib(directProvidedDep);

        final TsArtifact depC2 = TsArtifact.jar("dep-c", "2");
        // here provided dependencies will override compile/runtime ones during version convergence
        addToExpectedLib(depC2);
        directProvidedDep.addDependency(depC2);

        final TsArtifact transitiveProvidedDep = TsArtifact.jar("transitive-provided-dep");
        addToExpectedLib(transitiveProvidedDep);
        directProvidedDep.addDependency(transitiveProvidedDep);

        return TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addDependency(extA)
                .addDependency(extB, JavaScopes.PROVIDED)
                .addDependency(new TsDependency(directProvidedDep, JavaScopes.PROVIDED));
    }

    @Override
    protected void assertAppModel(ApplicationModel model) throws Exception {
        Set<Dependency> expected = new HashSet<>();
        expected.add(new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "ext-a-deployment", "1"),
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "ext-a-deployment-dep", "1"),
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "ext-b-deployment", "1"),
                JavaScopes.PROVIDED,
                DependencyFlags.DEPLOYMENT_CP));
        assertEquals(expected, getDeploymentOnlyDeps(model));

        expected = new HashSet<>();
        expected.add(new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "ext-a", "1"),
                DependencyFlags.RUNTIME_CP,
                DependencyFlags.DEPLOYMENT_CP,
                DependencyFlags.DIRECT,
                DependencyFlags.RUNTIME_EXTENSION_ARTIFACT,
                DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT));
        expected.add(new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "ext-a-dep", "1"),
                DependencyFlags.RUNTIME_CP,
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "dep-c", "2"),
                DependencyFlags.RUNTIME_CP,
                DependencyFlags.DEPLOYMENT_CP));
        expected.add(new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "ext-b", "1"),
                JavaScopes.PROVIDED,
                DependencyFlags.RUNTIME_EXTENSION_ARTIFACT,
                DependencyFlags.DIRECT,
                DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT,
                DependencyFlags.RUNTIME_CP,
                DependencyFlags.DEPLOYMENT_CP,
                DependencyFlags.COMPILE_ONLY));
        expected.add(new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "direct-provided-dep", "1"),
                JavaScopes.PROVIDED,
                DependencyFlags.DIRECT,
                DependencyFlags.RUNTIME_CP,
                DependencyFlags.DEPLOYMENT_CP,
                DependencyFlags.COMPILE_ONLY));
        expected.add(new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "transitive-provided-dep", "1"),
                JavaScopes.PROVIDED,
                DependencyFlags.RUNTIME_CP,
                DependencyFlags.DEPLOYMENT_CP,
                DependencyFlags.COMPILE_ONLY));
        assertEquals(expected, getDependenciesWithFlag(model, DependencyFlags.RUNTIME_CP));

        expected = new HashSet<>();
        expected.add(new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "ext-b", "1"),
                JavaScopes.PROVIDED,
                DependencyFlags.RUNTIME_EXTENSION_ARTIFACT,
                DependencyFlags.DIRECT,
                DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT,
                DependencyFlags.RUNTIME_CP,
                DependencyFlags.DEPLOYMENT_CP,
                DependencyFlags.COMPILE_ONLY));
        expected.add(new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "direct-provided-dep", "1"),
                JavaScopes.PROVIDED,
                DependencyFlags.DIRECT,
                DependencyFlags.RUNTIME_CP,
                DependencyFlags.DEPLOYMENT_CP,
                DependencyFlags.COMPILE_ONLY));
        expected.add(new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "transitive-provided-dep", "1"),
                JavaScopes.PROVIDED,
                DependencyFlags.RUNTIME_CP,
                DependencyFlags.DEPLOYMENT_CP,
                DependencyFlags.COMPILE_ONLY));
        assertEquals(expected, getDependenciesWithFlag(model, DependencyFlags.COMPILE_ONLY));
    }
}
