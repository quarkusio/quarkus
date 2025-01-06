package io.quarkus.deployment.runnerjar;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Profile;

import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;

public class DependencyAddedInProfileTest extends BootstrapFromOriginalJarTestBase {

    @Override
    protected boolean createWorkspace() {
        return true;
    }

    @Override
    protected TsArtifact composeApplication() {

        final TsQuarkusExt extA100 = new TsQuarkusExt("ext-a", "1.0.0");
        addToExpectedLib(extA100.getRuntime());

        final TsQuarkusExt extB100 = new TsQuarkusExt("ext-b", "1.0.0");
        install(extB100);
        final TsArtifact extB100Rt = extB100.getRuntime();
        addToExpectedLib(extB100Rt);

        final TsArtifact appJar = TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addDependency(extA100);

        final Profile profile = new Profile();
        profile.setId("extra");
        Activation activation = new Activation();
        ActivationProperty ap = new ActivationProperty();
        ap.setName("extra");
        activation.setProperty(ap);
        profile.setActivation(activation);
        final Dependency dep = new Dependency();
        dep.setGroupId(extB100Rt.getGroupId());
        dep.setArtifactId(extB100Rt.getArtifactId());
        dep.setVersion(extB100Rt.getVersion());
        profile.addDependency(dep);
        appJar.addProfile(profile);

        createWorkspace();

        setSystemProperty("extra", "extra");

        return appJar;
    }
}
