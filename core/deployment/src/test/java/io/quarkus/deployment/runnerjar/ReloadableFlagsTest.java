package io.quarkus.deployment.runnerjar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

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
import io.quarkus.maven.dependency.ResolvedDependency;

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

        for (ResolvedDependency dep : model.getDependencies()) {
            switch (dep.getArtifactId()) {
                case "acme-transitive":
                    assertThat(dep.getDirectDependencies()).isEmpty();
                    break;
                case "acme-common":
                    assertThat(dep.getDirectDependencies()).containsExactlyInAnyOrder(
                            Dependency.jarWithFlags(TsArtifact.DEFAULT_GROUP_ID, "acme-transitive", "1",
                                    DependencyFlags.DIRECT |
                                            DependencyFlags.RUNTIME_CP |
                                            DependencyFlags.DEPLOYMENT_CP |
                                            DependencyFlags.WORKSPACE_MODULE));
                    break;
                case "acme-lib":
                case "external-lib":
                    assertThat(dep.getDirectDependencies()).containsExactlyInAnyOrder(
                            Dependency.jarWithFlags(TsArtifact.DEFAULT_GROUP_ID, "acme-common", "1",
                                    DependencyFlags.DIRECT |
                                            DependencyFlags.RUNTIME_CP |
                                            DependencyFlags.DEPLOYMENT_CP |
                                            DependencyFlags.WORKSPACE_MODULE));
                    break;
                case "my-ext":
                    assertThat(dep.getDirectDependencies()).isEmpty();
                    break;
                case "my-ext-deployment":
                    assertThat(dep.getDirectDependencies()).containsExactlyInAnyOrder(
                            Dependency.jarWithFlags(TsArtifact.DEFAULT_GROUP_ID, "my-ext", "1",
                                    DependencyFlags.DIRECT |
                                            DependencyFlags.RUNTIME_CP |
                                            DependencyFlags.DEPLOYMENT_CP |
                                            DependencyFlags.RUNTIME_EXTENSION_ARTIFACT |
                                            DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT));
                    break;
                default:
                    fail("Unrecognized dependency " + dep.toCompactCoords());
            }
        }
    }
}
