package io.quarkus.deployment.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.deployment.builditem.ModuleExportBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JPMSExportBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.runtime.graal.GraalVM;

@SuppressWarnings("deprecation")
class LegacyModuleExportsTest {
    @Test
    void preservesLegacyExportAndIgnoresVersionRange() {
        var model = new ApplicationModelBuilder().setAppArtifact(ResolvedDependencyBuilder.newInstance()
                .setGroupId("org.acme").setArtifactId("app").setVersion("1.0")).build();
        List<ModuleExportBuildItem> exports = new ArrayList<>();
        new NativeImageFeatureStep().legacyModuleExports(List.of(new JPMSExportBuildItem("java.base", "java.lang",
                GraalVM.Version.VERSION_25_0_0, GraalVM.Version.VERSION_25_1_0)),
                new CurateOutcomeBuildItem(model), exports::add);
        assertThat(exports).hasSize(1);
        ModuleExportBuildItem export = exports.get(0);
        assertThat(export.exportedModuleName()).isEqualTo("java.base");
        assertThat(export.exportingModuleName()).isEqualTo(model.getAppArtifact().getModuleName());
        assertThat(export.packageNames()).containsExactly("java.lang");
    }
}
