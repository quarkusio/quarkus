package io.quarkus.analytics;

import java.nio.file.Path;
import java.util.List;

import org.mockito.Mockito;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.PlatformImports;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;

public abstract class AnalyticsServiceTestBase {

    protected ApplicationModel mockApplicationModel() {
        ApplicationModel applicationModel = Mockito.mock(ApplicationModel.class);

        PlatformImports platforms = Mockito.mock(PlatformImports.class);
        ResolvedDependency appArtifact = Mockito.mock(ResolvedDependency.class);

        Mockito.when(applicationModel.getAppArtifact()).thenReturn(appArtifact);
        Mockito.when(appArtifact.getGroupId()).thenReturn("build-group-id");
        Mockito.when(appArtifact.getArtifactId()).thenReturn("build-artifact-id");
        Mockito.when(appArtifact.getVersion()).thenReturn("1.0.0-TEST");
        Mockito.when(applicationModel.getPlatforms()).thenReturn(platforms);
        Mockito.when(platforms.getImportedPlatformBoms())
                .thenReturn(List.of(ArtifactCoords.of(
                        "quarkus-group", // the quarkus being used on the build
                        "quarkus",
                        "",
                        "",
                        "1.0.0-QUARKUSTEST")));
        Mockito.when(applicationModel.getDependencies())
                .thenReturn(List.of(
                        ResolvedDependencyBuilder.newInstance()
                                .setGroupId("io.quarkus")
                                .setArtifactId("quarkus-openapi") // will be ok
                                .setVersion("1.0.0-QUARKUSTEST")
                                .setRuntimeExtensionArtifact()
                                .setResolvedPath(Path.of("path/to/artifact.jar"))
                                .setFlags(DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT)
                                .build(),
                        ResolvedDependencyBuilder.newInstance()
                                .setGroupId("not.quarkus")
                                .setArtifactId("not-quarkus-openapi") // not a public extension
                                .setVersion("1.0.0-QUARKUSTEST")
                                .setRuntimeExtensionArtifact()
                                .setResolvedPath(Path.of("path/to/artifact.jar"))
                                .setFlags(DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT)
                                .build(),
                        ResolvedDependencyBuilder.newInstance()
                                .setGroupId("io.quarkiverse")
                                .setArtifactId("quarkus-opentelemetry-jaeger") // will be ok
                                .setVersion("1.0.0-QUARKUSTEST")
                                .setRuntimeExtensionArtifact()
                                .setResolvedPath(Path.of("path/to/artifact.jar"))
                                .setFlags(DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT)
                                .build(),
                        ResolvedDependencyBuilder.newInstance()
                                .setGroupId("io.quarkus")
                                .setArtifactId("quarkus-resteasy") // not a TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT
                                .setVersion("1.0.0-QUARKUSTEST")
                                .setRuntimeExtensionArtifact()
                                .setResolvedPath(Path.of("path/to/artifact.jar"))
                                .setFlags(DependencyFlags.OPTIONAL)
                                .build()));
        return applicationModel;
    }
}
