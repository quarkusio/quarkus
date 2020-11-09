package io.quarkus.platform.descriptor.loader.json;

import io.quarkus.bootstrap.resolver.AppModelResolverException;
import java.nio.file.Path;
import java.util.function.Function;

public interface ArtifactResolver {

    default <T> T process(String groupId, String artifactId, String version, Function<Path, T> processor)
            throws AppModelResolverException {
        return process(groupId, artifactId, null, "jar", version, processor);
    }

    <T> T process(String groupId, String artifactId, String classifier, String type, String version,
            Function<Path, T> processor) throws AppModelResolverException;
}
