package io.quarkus.deployment.runnerjar;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;

import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.sbom.ApplicationComponent;

public class ApplicationManifestUberJarTest extends ApplicationManifestTestBase {

    @Override
    protected TsArtifact composeApplication() {

        var acmeTransitive = TsArtifact.jar("acme-transitive");

        var acmeCommon = TsArtifact.jar("acme-common")
                .addDependency(acmeTransitive);

        var acmeLib = TsArtifact.jar("acme-lib")
                .addDependency(acmeCommon);

        var otherLib = TsArtifact.jar("other-lib");
        otherLib.addDependency(acmeCommon);

        var myLib = TsArtifact.jar("my-lib");
        myLib.addDependency(acmeCommon);

        var myExt = new TsQuarkusExt("my-ext");
        myExt.getRuntime().addDependency(myLib);
        myExt.getDeployment().addDependency(otherLib);

        return TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addDependency(acmeLib)
                .addDependency(otherLib)
                .addDependency(myExt);
    }

    @Override
    protected Properties buildSystemProperties() {
        var props = new Properties();
        props.setProperty("quarkus.package.jar.type", "uber-jar");
        return props;
    }

    @BeforeEach
    public void initExpectedComponents() {
        expectMavenComponent(artifactCoords("app", "runner"), comp -> {
            assertNoDistributionPath(comp);
            assertDependencies(comp,
                    artifactCoords("acme-lib"),
                    artifactCoords("other-lib"),
                    artifactCoords("my-ext"),
                    artifactCoords("my-ext-deployment"));
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });

        expectMavenComponent(artifactCoords("acme-lib"), comp -> {
            assertNoDistributionPath(comp);
            assertDependencies(comp, artifactCoords("acme-common"));
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });

        expectMavenComponent(artifactCoords("acme-common"), comp -> {
            assertNoDistributionPath(comp);
            assertDependencies(comp, artifactCoords("acme-transitive"));
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });

        expectMavenComponent(artifactCoords("acme-transitive"), comp -> {
            assertNoDistributionPath(comp);
            assertDependencies(comp);
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });

        expectMavenComponent(artifactCoords("other-lib"), comp -> {
            assertNoDistributionPath(comp);
            assertDependencies(comp, artifactCoords("acme-common"));
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });

        expectMavenComponent(artifactCoords("my-lib"), comp -> {
            assertNoDistributionPath(comp);
            assertDependencies(comp, artifactCoords("acme-common"));
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });

        expectMavenComponent(artifactCoords("my-ext"), comp -> {
            assertNoDistributionPath(comp);
            assertDependencies(comp, artifactCoords("my-lib"));
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });

        expectMavenComponent(artifactCoords("my-ext-deployment"), comp -> {
            assertNoDistributionPath(comp);
            assertDependencies(comp,
                    artifactCoords("my-ext"),
                    artifactCoords("other-lib"));
            assertDependencyScope(comp, ApplicationComponent.SCOPE_DEVELOPMENT);
        });
    }
}
