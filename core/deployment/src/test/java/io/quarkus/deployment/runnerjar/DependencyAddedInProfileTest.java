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

        final TsQuarkusExt extA_100 = new TsQuarkusExt("ext-a", "1.0.0");
        addToExpectedLib(extA_100.getRuntime());

        final TsQuarkusExt extB_100 = new TsQuarkusExt("ext-b", "1.0.0");
        install(extB_100);
        final TsArtifact extB_100_rt = extB_100.getRuntime();
        addToExpectedLib(extB_100_rt);

        final TsArtifact appJar = TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addDependency(extA_100);

        final Profile profile = new Profile();
        profile.setId("extra");
        Activation activation = new Activation();
        ActivationProperty ap = new ActivationProperty();
        ap.setName("extra");
        activation.setProperty(ap);
        profile.setActivation(activation);
        final Dependency dep = new Dependency();
        dep.setGroupId(extB_100_rt.getGroupId());
        dep.setArtifactId(extB_100_rt.getArtifactId());
        dep.setVersion(extB_100_rt.getVersion());
        profile.addDependency(dep);
        appJar.addProfile(profile);

        createWorkspace();

        setSystemProperty("extra", "extra");

        return appJar;
    }
}
