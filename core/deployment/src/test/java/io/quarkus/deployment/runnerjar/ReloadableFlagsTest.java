package io.quarkus.deployment.runnerjar;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.aether.util.artifact.JavaScopes;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.DependencyFlags;

public class ReloadableFlagsTest extends BootstrapFromOriginalJarTestBase {

    @Override
    protected TsArtifact composeApplication() {

        var transitive = TsArtifact.jar("acme-transitive");
        addWorkspaceModule(transitive);
        addToExpectedLib(transitive);

        var common = TsArtifact.jar("acme-common");
        common.addDependency(transitive);
        addWorkspaceModule(common);
        addToExpectedLib(common);

        var lib = TsArtifact.jar("acme-lib");
        lib.addDependency(common);
        addWorkspaceModule(lib);
        addToExpectedLib(lib);

        var externalLib = TsArtifact.jar("external-lib");
        externalLib.addDependency(common);
        addToExpectedLib(externalLib);

        var myExt = new TsQuarkusExt("my-ext");
        addToExpectedLib(myExt.getRuntime());

        return TsArtifact.jar("app")
                .addManagedDependency(platformDescriptor())
                .addManagedDependency(platformProperties())
                .addDependency(common)
                .addDependency(lib)
                .addDependency(externalLib)
                .addDependency(myExt);
    }

    @Override
    protected void assertAppModel(ApplicationModel model) {
        assertThat(model.getWorkspaceModules().stream().map(WorkspaceModule::getId).collect(Collectors.toSet()))
                .isEqualTo(Set.of(
                        WorkspaceModuleId.of(TsArtifact.DEFAULT_GROUP_ID, "acme-transitive", TsArtifact.DEFAULT_VERSION),
                        WorkspaceModuleId.of(TsArtifact.DEFAULT_GROUP_ID, "acme-common", TsArtifact.DEFAULT_VERSION),
                        WorkspaceModuleId.of(TsArtifact.DEFAULT_GROUP_ID, "acme-lib", TsArtifact.DEFAULT_VERSION),
                        WorkspaceModuleId.of(TsArtifact.DEFAULT_GROUP_ID, "app", TsArtifact.DEFAULT_VERSION)));

        final Set<Dependency> expected = Set.of(
                new ArtifactDependency(ArtifactCoords.jar("io.quarkus.bootstrap.test", "acme-lib", "1"), JavaScopes.COMPILE,
                        DependencyFlags.RUNTIME_CP, DependencyFlags.DEPLOYMENT_CP, DependencyFlags.DIRECT,
                        DependencyFlags.RELOADABLE, DependencyFlags.WORKSPACE_MODULE));

        assertThat(getDependenciesWithFlag(model, DependencyFlags.RELOADABLE)).isEqualTo(expected);
    }
}
