package io.quarkus.jacoco.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.jandex.IndexView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.pkg.builditem.BuildSystemTargetBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.jacoco.runtime.JacocoConfig;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 * Regression for a selected application module being left out of the JaCoCo
 * report input list. The list is written during augmentation, before the
 * shutdown hook generates jacoco.xml.
 */
@ResourceLock("io.quarkus.internal.jacoco.report-data-file")
class JacocoSelectedApplicationReportTest {

    private static final String REPORT_DATA_PROPERTY = "io.quarkus.internal.jacoco.report-data-file";

    @TempDir
    Path tempDir;

    @Test
    void selectedApplicationModuleContributesClassesToReport() throws Exception {
        Path classFile = runProcessor(ArtifactKey.of("org.acme", "app"));
        Path list = tempDir.resolve("target/report-classes.txt");

        assertThat(Files.readAllLines(list))
                .contains(classFile.toAbsolutePath().toString());
    }

    @Test
    void dependencyOnlySelectionDoesNotAddApplicationClasses() throws Exception {
        Path classFile = runProcessor(ArtifactKey.of("org.example", "selected-dependency"));
        Path list = tempDir.resolve("target/report-classes.txt");

        assertThat(Files.readAllLines(list))
                .doesNotContain(classFile.toAbsolutePath().toString());
    }

    private Path runProcessor(ArtifactKey selectedKey) throws Exception {
        ArtifactKey applicationKey = ArtifactKey.of("org.acme", "app");
        Path sourceDir = Files.createDirectories(tempDir.resolve("src"));
        Path classesDir = Files.createDirectories(tempDir.resolve("classes"));
        Path classFile = classesDir.resolve("io/quarkus/jacoco/deployment/JacocoSelectedApplicationReportTest.class");
        Files.createDirectories(classFile.getParent());
        try (InputStream input = getClass().getResourceAsStream("JacocoSelectedApplicationReportTest.class")) {
            assertThat(input).isNotNull();
            Files.copy(input, classFile);
        }

        WorkspaceModule workspace = mock(WorkspaceModule.class);
        when(workspace.getModuleDir()).thenReturn(tempDir.toFile());

        ArtifactSources sources = mock(ArtifactSources.class);
        when(sources.getSourceDirs()).thenReturn(List.of(SourceDir.of(sourceDir, classesDir)));

        ResolvedDependency application = mock(ResolvedDependency.class);
        when(application.getKey()).thenReturn(applicationKey);
        when(application.getWorkspaceModule()).thenReturn(workspace);
        when(application.getSources()).thenReturn(sources);

        ApplicationModel model = mock(ApplicationModel.class);
        when(model.getAppArtifact()).thenReturn(application);
        when(model.getApplicationModule()).thenReturn(workspace);
        when(model.getRuntimeDependencies()).thenReturn(List.of());

        ApplicationArchive rootArchive = mock(ApplicationArchive.class);
        when(rootArchive.getKey()).thenReturn(applicationKey);
        IndexView emptyIndex = mock(IndexView.class);
        when(emptyIndex.getKnownClasses()).thenReturn(List.of());
        when(rootArchive.getIndex()).thenReturn(emptyIndex);

        ApplicationArchive dependencyArchive = mock(ApplicationArchive.class);
        when(dependencyArchive.getKey()).thenReturn(ArtifactKey.of("org.example", "selected-dependency"));
        when(dependencyArchive.getIndex()).thenReturn(emptyIndex);

        ApplicationArchivesBuildItem archives = mock(ApplicationArchivesBuildItem.class);
        when(archives.getAllArchives()).thenReturn(List.of(rootArchive, dependencyArchive));

        CurateOutcomeBuildItem curate = mock(CurateOutcomeBuildItem.class);
        when(curate.getApplicationModel()).thenReturn(model);

        OutputTargetBuildItem output = mock(OutputTargetBuildItem.class);
        when(output.getOutputDirectory()).thenReturn(tempDir.resolve("target"));

        BuildSystemTargetBuildItem buildTarget = mock(BuildSystemTargetBuildItem.class);
        when(buildTarget.getBaseName()).thenReturn("coverage-test");

        LaunchModeBuildItem launchMode = mock(LaunchModeBuildItem.class);
        when(launchMode.isTest()).thenReturn(true);

        JacocoConfig.IndexDependencyConfig selection = mock(JacocoConfig.IndexDependencyConfig.class);
        when(selection.groupId()).thenReturn(selectedKey.getGroupId());
        when(selection.artifactId()).thenReturn(Optional.of(selectedKey.getArtifactId()));
        when(selection.classifier()).thenReturn(Optional.empty());

        JacocoConfig config = mock(JacocoConfig.class);
        when(config.enabled()).thenReturn(true);
        when(config.report()).thenReturn(true);
        when(config.instrumentArtifacts()).thenReturn(Map.of("selection", selection));
        when(config.dataFile()).thenReturn(Optional.empty());
        when(config.reportLocation()).thenReturn(Optional.empty());
        when(config.includes()).thenReturn(List.of("**/*"));
        when(config.excludes()).thenReturn(Optional.empty());
        when(config.title()).thenReturn(Optional.empty());
        when(config.footer()).thenReturn(Optional.empty());
        when(config.outputEncoding()).thenReturn("UTF-8");
        when(config.sourceEncoding()).thenReturn("UTF-8");

        String oldReportFile = System.getProperty(REPORT_DATA_PROPERTY);
        String oldAgentFile = System.getProperty("jacoco-agent.destfile");
        String oldJmx = System.getProperty("jacoco-agent.jmx");
        try {
            System.clearProperty(REPORT_DATA_PROPERTY);
            BuildProducer<BytecodeTransformerBuildItem> noOpProducer = item -> {};
            new JacocoProcessor().transform(noOpProducer, output, archives, buildTarget, curate, launchMode, config);
        } finally {
            restoreProperty(REPORT_DATA_PROPERTY, oldReportFile);
            restoreProperty("jacoco-agent.destfile", oldAgentFile);
            restoreProperty("jacoco-agent.jmx", oldJmx);
        }
        return classFile;
    }

    private static void restoreProperty(String name, String priorValue) {
        if (priorValue == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, priorValue);
        }
    }
}
