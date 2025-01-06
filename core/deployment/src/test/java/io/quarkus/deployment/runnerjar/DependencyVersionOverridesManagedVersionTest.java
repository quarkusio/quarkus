package io.quarkus.deployment.runnerjar;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsDependency;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;

public class DependencyVersionOverridesManagedVersionTest extends BootstrapFromOriginalJarTestBase {

    @Override
    protected boolean createWorkspace() {
        return true;
    }

    @Override
    protected boolean workspaceModuleParentHierarchy() {
        // this is to simply make sure the workspace modules available
        // through ApplicationModel.getWorkspaceModules() include parent POMs and BOMs
        return true;
    }

    @Override
    protected TsArtifact composeApplication() {

        final TsQuarkusExt extA100 = new TsQuarkusExt("ext-a", "1.0.0");
        install(extA100);

        final TsQuarkusExt extB100 = new TsQuarkusExt("ext-b", "1.0.0");
        install(extB100);
        final TsArtifact extB100Rt = extB100.getRuntime();
        addToExpectedLib(extB100Rt);

        final TsArtifact bom = TsArtifact.pom("test-bom");
        bom.addManagedDependency(platformDescriptor());
        bom.addManagedDependency(platformProperties());
        bom.addManagedDependency(new TsDependency(extA100.getRuntime()));
        bom.addManagedDependency(new TsDependency(extB100Rt));
        install(bom);

        final TsQuarkusExt extA101 = new TsQuarkusExt("ext-a", "1.0.1");
        install(extA101);
        addToExpectedLib(extA101.getRuntime());

        createWorkspace();

        return TsArtifact.jar("app")
                .addManagedDependency(new TsDependency(bom, "import"))
                .addDependency(new TsDependency(new TsArtifact(extB100Rt.getGroupId(), extB100Rt.getArtifactId(),
                        extB100Rt.getClassifier(), extB100Rt.getType(), null)))
                .addDependency(extA101.getRuntime());
    }

    @Override
    protected void assertAppModel(ApplicationModel model) {
        assertThat(model.getWorkspaceModules().stream().map(WorkspaceModule::getId).collect(Collectors.toSet()))
                .isEqualTo(Set.of(
                        WorkspaceModuleId.of(TsArtifact.DEFAULT_GROUP_ID, "app-parent", TsArtifact.DEFAULT_VERSION),
                        WorkspaceModuleId.of(TsArtifact.DEFAULT_GROUP_ID, "test-bom", TsArtifact.DEFAULT_VERSION),
                        WorkspaceModuleId.of(TsArtifact.DEFAULT_GROUP_ID, "app", TsArtifact.DEFAULT_VERSION),
                        WorkspaceModuleId.of(TsArtifact.DEFAULT_GROUP_ID, "ext-a", "1.0.1"),
                        WorkspaceModuleId.of(TsArtifact.DEFAULT_GROUP_ID, "ext-b", "1.0.0")));
    }
}
