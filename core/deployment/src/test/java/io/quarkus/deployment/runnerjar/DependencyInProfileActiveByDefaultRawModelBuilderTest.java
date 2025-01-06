package io.quarkus.deployment.runnerjar;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.model.Activation;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Profile;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.maven.dependency.ResolvedDependency;

public class DependencyInProfileActiveByDefaultRawModelBuilderTest extends BootstrapFromOriginalJarTestBase {

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

        final TsArtifact commonLibraryJar = TsArtifact.jar("common-library");
        addToExpectedLib(commonLibraryJar);
        addWorkspaceModule(commonLibraryJar);

        final TsArtifact module1Jar = TsArtifact.jar("module-one");
        module1Jar.addDependency(commonLibraryJar);
        addToExpectedLib(module1Jar);

        final TsArtifact module2Jar = TsArtifact.jar("module-two");
        module2Jar.addDependency(commonLibraryJar);
        addToExpectedLib(module2Jar);
        install(module2Jar);
        addWorkspaceModuleToProfile(module2Jar, "extra");

        final TsArtifact appJar = TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addDependency(extA100)
                .addDependency(module1Jar);

        final Profile profile = new Profile();
        profile.setId("extra");
        Activation activation = new Activation();
        activation.setActiveByDefault(true);
        profile.setActivation(activation);

        Dependency dep = new Dependency();
        dep.setGroupId(extB100Rt.getGroupId());
        dep.setArtifactId(extB100Rt.getArtifactId());
        dep.setVersion(extB100Rt.getVersion());
        profile.addDependency(dep);

        dep = new Dependency();
        dep.setGroupId(module2Jar.getGroupId());
        dep.setArtifactId(module2Jar.getArtifactId());
        dep.setVersion(module2Jar.getVersion());
        profile.addDependency(dep);

        appJar.addProfile(profile);

        createWorkspace();

        return appJar;
    }

    @Override
    protected void assertAppModel(ApplicationModel model) {

        final Map<String, ResolvedDependency> deps = new HashMap<>();
        model.getDependencies().forEach(d -> deps.put(d.getArtifactId(), d));
        assertThat(deps).hasSize(7);

        ResolvedDependency d = deps.get("module-one");
        assertThat(d).isNotNull();
        assertThat(d.isRuntimeCp()).isTrue();
        assertThat(d.isDeploymentCp()).isTrue();
        assertThat(d.isWorkspaceModule()).isTrue();
        assertThat(d.isReloadable()).isTrue();
        assertThat(d.isRuntimeExtensionArtifact()).isFalse();

        d = deps.get("module-two");
        assertThat(d).isNotNull();
        assertThat(d.isRuntimeCp()).isTrue();
        assertThat(d.isDeploymentCp()).isTrue();
        assertThat(d.isWorkspaceModule()).isTrue();
        assertThat(d.isReloadable()).isTrue();
        assertThat(d.isRuntimeExtensionArtifact()).isFalse();

        d = deps.get("common-library");
        assertThat(d).isNotNull();
        assertThat(d.isRuntimeCp()).isTrue();
        assertThat(d.isDeploymentCp()).isTrue();
        assertThat(d.isWorkspaceModule()).isTrue();
        assertThat(d.isReloadable()).isTrue();
        assertThat(d.isRuntimeExtensionArtifact()).isFalse();

        d = deps.get("ext-a");
        assertThat(d).isNotNull();
        assertThat(d.isRuntimeExtensionArtifact()).isTrue();
        assertThat(d.isRuntimeCp()).isTrue();
        assertThat(d.isDeploymentCp()).isTrue();
        assertThat(d.isWorkspaceModule()).isTrue();
        assertThat(d.isReloadable()).isFalse();

        d = deps.get("ext-b");
        assertThat(d).isNotNull();
        assertThat(d.isRuntimeExtensionArtifact()).isTrue();
        assertThat(d.isRuntimeCp()).isTrue();
        assertThat(d.isDeploymentCp()).isTrue();
        assertThat(d.isWorkspaceModule()).isFalse();
        assertThat(d.isReloadable()).isFalse();

        d = deps.get("ext-a-deployment");
        assertThat(d).isNotNull();
        assertThat(d.isRuntimeExtensionArtifact()).isFalse();
        assertThat(d.isRuntimeCp()).isFalse();
        assertThat(d.isDeploymentCp()).isTrue();
        assertThat(d.isWorkspaceModule()).isFalse();
        assertThat(d.isReloadable()).isFalse();

        d = deps.get("ext-b-deployment");
        assertThat(d).isNotNull();
        assertThat(d.isRuntimeExtensionArtifact()).isFalse();
        assertThat(d.isRuntimeCp()).isFalse();
        assertThat(d.isDeploymentCp()).isTrue();
        assertThat(d.isWorkspaceModule()).isFalse();
        assertThat(d.isReloadable()).isFalse();
    }
}
