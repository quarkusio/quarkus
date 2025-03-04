package io.quarkus.deployment.runnerjar;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;

import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.sbom.ApplicationComponent;

public class ApplicationManifestMutableJarTest extends ApplicationManifestTestBase {

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
        props.setProperty("quarkus.package.jar.type", "mutable-jar");
        return props;
    }

    @BeforeEach
    public void initExpectedComponents() {
        expectMavenComponent(artifactCoords("app"), comp -> {
            assertDistributionPath(comp, "app/quarkus-application.jar");
            assertDependencies(comp,
                    artifactCoords("acme-lib"),
                    artifactCoords("other-lib"),
                    artifactCoords("my-ext"),
                    artifactCoords("my-ext-deployment"));
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });

        expectMavenComponent(artifactCoords("acme-lib"), comp -> {
            assertDistributionPath(comp, "lib/main/io.quarkus.bootstrap.test.acme-lib-1.jar");
            assertDependencies(comp, artifactCoords("acme-common"));
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });

        expectMavenComponent(artifactCoords("acme-common"), comp -> {
            assertDistributionPath(comp, "lib/main/io.quarkus.bootstrap.test.acme-common-1.jar");
            assertDependencies(comp, artifactCoords("acme-transitive"));
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });

        expectMavenComponent(artifactCoords("acme-transitive"), comp -> {
            assertDistributionPath(comp, "lib/main/io.quarkus.bootstrap.test.acme-transitive-1.jar");
            assertDependencies(comp);
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });

        expectMavenComponent(artifactCoords("other-lib"), comp -> {
            assertDistributionPath(comp, "lib/main/io.quarkus.bootstrap.test.other-lib-1.jar");
            assertDependencies(comp, artifactCoords("acme-common"));
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });

        expectMavenComponent(artifactCoords("my-lib"), comp -> {
            assertDistributionPath(comp, "lib/main/io.quarkus.bootstrap.test.my-lib-1.jar");
            assertDependencies(comp, artifactCoords("acme-common"));
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });

        expectMavenComponent(artifactCoords("my-ext"), comp -> {
            assertDistributionPath(comp, "lib/main/io.quarkus.bootstrap.test.my-ext-1.jar");
            assertDependencies(comp, artifactCoords("my-lib"));
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });

        expectFileComponent("quarkus-run.jar", comp -> {
            assertDependencies(comp, artifactCoords("app"));
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });

        expectFileComponent("quarkus/generated-bytecode.jar", comp -> {
            assertDependencies(comp);
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });

        expectFileComponent("quarkus/quarkus-application.dat", comp -> {
            assertDependencies(comp);
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });

        expectFileComponent("quarkus-app-dependencies.txt", comp -> {
            assertDependencies(comp);
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });

        expectFileComponent("quarkus/build-system.properties", comp -> {
            assertDependencies(comp);
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });

        expectMavenComponent(artifactCoords("my-ext-deployment"), comp -> {
            assertDistributionPath(comp, "lib/deployment/io.quarkus.bootstrap.test.my-ext-deployment-1.jar");
            assertDependencyScope(comp, ApplicationComponent.SCOPE_DEVELOPMENT);
            assertDependencies(comp,
                    artifactCoords("my-ext"),
                    artifactCoords("other-lib"));
        });

        expectFileComponent("lib/deployment/appmodel.dat", comp -> {
            assertDependencies(comp);
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });

        expectFileComponent("lib/deployment/deployment-class-path.dat", comp -> {
            assertDependencies(comp);
            assertDependencyScope(comp, ApplicationComponent.SCOPE_RUNTIME);
        });
    }
}
