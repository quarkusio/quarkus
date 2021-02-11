package io.quarkus.platform.descriptor.resolver.json;

import io.quarkus.devtools.messagewriter.MessageWriter;
import java.nio.file.Path;

public interface JsonMavenArtifactResolver {

    Path resolveArtifact(String groupId, String artifactId, String classifier, String type, String version, MessageWriter log)
            throws Exception;
}
