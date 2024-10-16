package io.quarkus.deployment.runnerjar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.eclipse.aether.util.artifact.JavaScopes;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.maven.dependency.*;

public class DeploymentDependencyConvergenceTest extends BootstrapFromOriginalJarTestBase {

    @Override
    protected TsArtifact composeApplication() {

        var libE10 = install(TsArtifact.jar("lib-e", "1.0"));
        var libE20 = install(TsArtifact.jar("lib-e", "2.0"));
        var libE30 = install(TsArtifact.jar("lib-e", "3.0"));

        var libD10 = install(TsArtifact.jar("lib-d", "1.0"));
        var libD20 = install(TsArtifact.jar("lib-d", "2.0"));

        var libC10 = install(TsArtifact.jar("lib-c", "1.0")
                .addDependency(libD10)
                .addDependency(libE10));

        var libB10 = install(TsArtifact.jar("lib-b", "1.0"));
        var libB20 = install(TsArtifact.jar("lib-b", "2.0")
                .addDependency(libC10));

        var extA = new TsQuarkusExt("ext-a");
        addToExpectedLib(extA.getRuntime());
        extA.getDeployment()
                .addManagedDependency(libD20)
                .addManagedDependency(libE20)
                .addDependency(libB10);

        return TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addManagedDependency(libB20)
                .addManagedDependency(libE30)
                .addDependency(extA);
    }

    @Override
    protected void assertAppModel(ApplicationModel model) throws Exception {
        final Set<Dependency> expected = Set.of(
                new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "ext-a", "1"), JavaScopes.COMPILE,
                        DependencyFlags.RUNTIME_CP, DependencyFlags.DEPLOYMENT_CP, DependencyFlags.DIRECT,
                        DependencyFlags.RUNTIME_EXTENSION_ARTIFACT, DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT),
                new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "ext-a-deployment", "1"),
                        JavaScopes.COMPILE,
                        DependencyFlags.DEPLOYMENT_CP),
                new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "lib-b", "2.0"), JavaScopes.COMPILE,
                        DependencyFlags.DEPLOYMENT_CP),
                new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "lib-c", "1.0"), JavaScopes.COMPILE,
                        DependencyFlags.DEPLOYMENT_CP),
                new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "lib-d", "2.0"), JavaScopes.COMPILE,
                        DependencyFlags.DEPLOYMENT_CP),
                new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "lib-e", "3.0"), JavaScopes.COMPILE,
                        DependencyFlags.DEPLOYMENT_CP));
        assertEquals(expected, getDependenciesWithFlag(model, DependencyFlags.DEPLOYMENT_CP));
    }
}
