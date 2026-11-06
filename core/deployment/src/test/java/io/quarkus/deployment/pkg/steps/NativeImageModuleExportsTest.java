package io.quarkus.deployment.pkg.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.ModuleExportBuildItem;

class NativeImageModuleExportsTest {
    @Test
    void exportsAllPackagesAndDeduplicatesTargets() {
        List<String> args = new ArrayList<>();
        NativeImageBuildStep.addModuleExports(args, List.of(
                new ModuleExportBuildItem("java.base", "first.module", "java.lang", "java.io"),
                new ModuleExportBuildItem("java.base", "second.module", "java.lang")));
        assertThat(args).containsExactly("-J--add-exports=java.base/java.io=ALL-UNNAMED",
                "-J--add-exports=java.base/java.lang=ALL-UNNAMED");
    }

    @Test
    void includesExportsFromDifferentToolVersions() {
        List<String> args = new ArrayList<>();
        NativeImageBuildStep.addModuleExports(args, List.of(
                new ModuleExportBuildItem("old.module", "app.module", "old.package"),
                new ModuleExportBuildItem("new.module", "app.module", "new.package")));
        assertThat(args).containsExactly("-J--add-exports=new.module/new.package=ALL-UNNAMED",
                "-J--add-exports=old.module/old.package=ALL-UNNAMED");
    }
}
