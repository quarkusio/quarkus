package io.quarkus.deployment.pkg.builditem;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class JvmStartupOptimizerArchiveContractTest {

    @Test
    void describesEveryArchiveType() {
        assertThat(JvmStartupOptimizerArchiveType.AppCDS.getJvmFlag()).isEqualTo("-XX:SharedArchiveFile");
        assertThat(JvmStartupOptimizerArchiveType.AppCDS.getDefaultName()).isEqualTo("app-cds.jsa");
        assertThat(JvmStartupOptimizerArchiveType.AppCDS.getArtifactKind())
                .isEqualTo(JvmStartupOptimizerArchiveKind.FILE);
        assertThat(JvmStartupOptimizerArchiveType.AppCDS.renderRuntimeOption("/work/app-cds.jsa"))
                .isEqualTo("-XX:SharedArchiveFile=/work/app-cds.jsa");

        assertThat(JvmStartupOptimizerArchiveType.AOT.getJvmFlag()).isEqualTo("-XX:AOTCache");
        assertThat(JvmStartupOptimizerArchiveType.AOT.getDefaultName()).isEqualTo("app.aot");
        assertThat(JvmStartupOptimizerArchiveType.AOT.getArtifactKind())
                .isEqualTo(JvmStartupOptimizerArchiveKind.FILE);
        assertThat(JvmStartupOptimizerArchiveType.AOT.renderRuntimeOption("/work/app.aot"))
                .isEqualTo("-XX:AOTCache=/work/app.aot");

        assertThat(JvmStartupOptimizerArchiveType.SCC.getJvmFlag()).isEqualTo("-Xshareclasses:cacheDir");
        assertThat(JvmStartupOptimizerArchiveType.SCC.getDefaultName()).isEqualTo("app-scc");
        assertThat(JvmStartupOptimizerArchiveType.SCC.getArtifactKind())
                .isEqualTo(JvmStartupOptimizerArchiveKind.DIRECTORY);
        assertThat(JvmStartupOptimizerArchiveType.SCC.renderRuntimeOption("/work/app-scc"))
                .isEqualTo("-Xshareclasses:name=quarkus-app,cacheDir=/work/app-scc,readonly");
    }

    @Test
    void pathOnlyRequestRetainsAotSemantics() {
        Path archive = Path.of("app.aot");

        var request = new BuildAotOptimizedContainerImageRequestBuildItem(
                "quay.io/acme/example:1.0", "/work", archive);

        assertThat(request.getOriginalContainerImage()).isEqualTo("quay.io/acme/example:1.0");
        assertThat(request.getContainerWorkingDirectory()).isEqualTo("/work");
        assertThat(request.getArchiveType()).isEqualTo(JvmStartupOptimizerArchiveType.AOT);
        assertThat(request.getArchive()).isEqualTo(archive);
        assertThat(request.getAotFile()).isEqualTo(archive);
        assertThat(request.getArtifactKind()).isEqualTo(JvmStartupOptimizerArchiveKind.FILE);
    }

    @Test
    void typedRequestDerivesDirectoryKind() {
        Path archive = Path.of("app-scc");

        var request = new BuildAotOptimizedContainerImageRequestBuildItem(
                "quay.io/acme/example:1.0", "/work", JvmStartupOptimizerArchiveType.SCC, archive);

        assertThat(request.getArchiveType()).isEqualTo(JvmStartupOptimizerArchiveType.SCC);
        assertThat(request.getArchive()).isEqualTo(archive);
        assertThat(request.getArtifactKind()).isEqualTo(JvmStartupOptimizerArchiveKind.DIRECTORY);
    }
}
