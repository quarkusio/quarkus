package io.quarkus.gradle;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gradle.tooling.BuildException;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.bootstrap.resolver.QuarkusGradleModelFactory;
import io.quarkus.bootstrap.resolver.model.Dependency;
import io.quarkus.bootstrap.resolver.model.QuarkusModel;
import io.quarkus.runtime.LaunchMode;

public final class ConnectorDependencyResolver {

    private List<AppArtifactCoords> dependencies = null;

    List<AppArtifactCoords> getDependencies(String buildContent, Path projectDirPath) {
        if (dependencies == null) {
            QuarkusModel quarkusModel = null;
            if (buildContent != null) {
                try {
                    quarkusModel = QuarkusGradleModelFactory.create(projectDirPath.toFile(), LaunchMode.NORMAL.toString());
                } catch (BuildException e) {
                    // ignore this error.
                    e.printStackTrace();
                }
            }
            if (quarkusModel != null) {
                dependencies = Stream
                        .concat(Stream.concat(quarkusModel.getAppDependencies().stream(),
                                quarkusModel.getExtensionDependencies().stream()),
                                quarkusModel.getEnforcedPlatformDependencies().stream())
                        .distinct().map(this::gradleModuleVersionToDependency).collect(Collectors.toList());
            } else {
                dependencies = Collections.emptyList();
            }
            dependencies = Collections.unmodifiableList(dependencies);
        }
        return dependencies;
    }

    private AppArtifactCoords gradleModuleVersionToDependency(Dependency quarkusDependency) {
        return new AppArtifactCoords(quarkusDependency.getGroupId(),
                quarkusDependency.getName(), quarkusDependency.getType(), quarkusDependency.getVersion());
    }
}
