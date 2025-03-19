package io.quarkus.deployment.runnerjar;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.TsArtifact;
import io.quarkus.bootstrap.resolver.TsDependency;
import io.quarkus.bootstrap.resolver.TsQuarkusExt;
import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.ResolvedDependency;

public class BasicBuildFromWorkspaceModuleTest extends BootstrapFromWorkspaceModuleTestBase {

    @Override
    protected WorkspaceModule composeApplication() throws Exception {

        // Acme Quarkus extension
        final TsQuarkusExt acmeExt = new TsQuarkusExt("acme-ext");
        install(acmeExt);
        addToExpectedLib(acmeExt.getRuntime());

        // Acme Quarkus platform BOM
        final TsArtifact acmeBom = TsArtifact.pom("org.acme", "acme-bom", "1.0");
        acmeBom.addManagedDependency(platformDescriptor());
        acmeBom.addManagedDependency(platformProperties());
        acmeBom.addManagedDependency(new TsDependency(acmeExt.getRuntime()));
        install(acmeBom);

        final WorkspaceModule module = WorkspaceModule.builder()
                .setModuleId(WorkspaceModuleId.of("org.acme", "acme-app", "1"))
                .setModuleDir(mkdir("app-module"))
                .setBuildDir(mkdir("target"))
                .addArtifactSources(ArtifactSources.main(
                        SourceDir.of(mkdir("app-module/main/java"), mkdir("main/classes")),
                        SourceDir.of(mkdir("app-module/main/resources"), mkdir("main/classes"))))
                .addArtifactSources(ArtifactSources.test(
                        SourceDir.of(mkdir("app-module/test/java"), mkdir("test/classes")),
                        SourceDir.of(mkdir("app-module/test/resources"), mkdir("test/classes"))))
                .addDependencyConstraint(
                        Dependency.pomImport(acmeBom.getGroupId(), acmeBom.getArtifactId(), acmeBom.getVersion()))
                .addDependency(Dependency.of(acmeExt.getRuntime().getGroupId(), acmeExt.getRuntime().getArtifactId()))
                .build();

        return module;
    }

    @Override
    protected void assertAppModel(ApplicationModel appModel) throws Exception {

        final ResolvedDependency appArtifact = appModel.getAppArtifact();
        assertThat(appArtifact.toCompactCoords()).isEqualTo("org.acme:acme-app:1");
        assertThat(appArtifact.getResolvedPaths().getSinglePath()).isEqualTo(workDir.resolve("main/classes"));

        // runtime classpath
        assertThat(appModel.getDependencies().stream().filter(Dependency::isRuntimeCp).map(ArtifactCoords::getArtifactId)
                .collect(Collectors.toList())).isEqualTo(List.of("acme-ext"));

        // deployment classpath
        assertThat(appModel.getDependencies().stream().filter(Dependency::isDeploymentCp).map(ArtifactCoords::getArtifactId)
                .collect(Collectors.toList())).isEqualTo(List.of("acme-ext", "acme-ext-deployment"));
    }

    private Path mkdir(String path) throws IOException {
        Path p = workDir.resolve(path);
        Files.createDirectories(p);
        return p;
    }
}
