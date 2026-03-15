package io.quarkus.deployment.runnerjar;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;

import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.sbom.ApplicationComponent;

public class ApplicationManifestFastJarTest extends ApplicationManifestTestBase {

    @Override
    protected TsArtifact composeApplication() {

        var acmeTransitive = TsArtifact.jar("acme-transitive");

        var acmeCommon = TsArtifact.jar("acme-common", "3.0")
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

        return TsArtifact.jar("app", "2.0")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addDependency(acmeLib)
                .addDependency(otherLib)
                .addDependency(myExt);
    }

    @Override
    protected Properties buildSystemProperties() {
        var props = new Properties();
        props.setProperty("quarkus.package.jar.type", "fast-jar");
        return props;
    }

    @BeforeEach
    public void initExpectedComponents() {

        expectMavenComponent(ArtifactCoords.jar(TsArtifact.DEFAULT_GROUP_ID, "app", "2.0"), comp -> {
            assertDistributionPath(comp, "app/quarkus-application.jar");
            assertVersion(comp, "2.0");
            assertDependencies(comp,
                    artifactCoords("acme-lib"),
                    artifactCoords("other-lib"),
                    artifactCoords("my-ext"),
                    artifactCoords("my-ext-deployment"));
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });

        final ArtifactCoords commonsCoords = ArtifactCoords.jar(TsArtifact.DEFAULT_GROUP_ID, "acme-common", "3.0");

        expectMavenComponent(artifactCoords("acme-lib"), comp -> {
            assertDistributionPath(comp, "lib/main/io.quarkus.bootstrap.test.acme-lib-1.jar");
            assertVersion(comp, TsArtifact.DEFAULT_VERSION);
            assertDependencies(comp, commonsCoords);
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });

        expectMavenComponent(commonsCoords, comp -> {
            assertDistributionPath(comp, "lib/main/io.quarkus.bootstrap.test.acme-common-3.0.jar");
            assertVersion(comp, "3.0");
            assertDependencies(comp, artifactCoords("acme-transitive"));
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });

        expectMavenComponent(artifactCoords("acme-transitive"), comp -> {
            assertDistributionPath(comp, "lib/main/io.quarkus.bootstrap.test.acme-transitive-1.jar");
            assertVersion(comp, TsArtifact.DEFAULT_VERSION);
            assertDependencies(comp);
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });

        expectMavenComponent(artifactCoords("other-lib"), comp -> {
            assertDistributionPath(comp, "lib/main/io.quarkus.bootstrap.test.other-lib-1.jar");
            assertVersion(comp, TsArtifact.DEFAULT_VERSION);
            assertDependencies(comp, commonsCoords);
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });

        expectMavenComponent(artifactCoords("my-lib"), comp -> {
            assertDistributionPath(comp, "lib/main/io.quarkus.bootstrap.test.my-lib-1.jar");
            assertVersion(comp, TsArtifact.DEFAULT_VERSION);
            assertDependencies(comp, commonsCoords);
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });

        expectMavenComponent(artifactCoords("my-ext"), comp -> {
            assertDistributionPath(comp, "lib/main/io.quarkus.bootstrap.test.my-ext-1.jar");
            assertVersion(comp, TsArtifact.DEFAULT_VERSION);
            assertDependencies(comp, artifactCoords("my-lib"));
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });

        expectFileComponent("quarkus-run.jar", comp -> {
            assertVersion(comp, "2.0");
            assertDependencies(comp, ArtifactCoords.jar(TsArtifact.DEFAULT_GROUP_ID, "app", "2.0"));
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });

        expectFileComponent("quarkus/generated-bytecode.jar", comp -> {
            assertVersion(comp, "2.0");
            assertDependencies(comp);
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });

        expectFileComponent("quarkus/quarkus-application.dat", comp -> {
            assertVersion(comp, "2.0");
            assertDependencies(comp);
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });

        expectFileComponent("quarkus-app-dependencies.txt", comp -> {
            assertVersion(comp, "2.0");
            assertDependencies(comp);
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });

        expectMavenComponent(artifactCoords("my-ext-deployment"), comp -> {
            assertNoDistributionPath(comp);
            assertVersion(comp, TsArtifact.DEFAULT_VERSION);
            assertDependencies(comp,
                    artifactCoords("my-ext"),
                    artifactCoords("other-lib"));
            assertDependencyScope(comp, ApplicationComponent.SCOPE_DEVELOPMENT);
        });
    }
}
