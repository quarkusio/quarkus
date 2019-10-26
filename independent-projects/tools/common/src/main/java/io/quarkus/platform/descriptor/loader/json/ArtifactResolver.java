package io.quarkus.platform.descriptor.loader.json;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import org.apache.maven.model.Dependency;

public interface ArtifactResolver {

    List<Dependency> getManagedDependencies(String groupId, String artifactId, String version);

    default <T> T process(String groupId, String artifactId, String version, Function<Path, T> processor) {
        return process(groupId, artifactId, null, "jar", version, processor);
    }

    <T> T process(String groupId, String artifactId, String classifier, String type, String version, Function<Path, T> processor);
}
