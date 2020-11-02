package io.quarkus.platform.descriptor.resolver.json;

import io.quarkus.devtools.messagewriter.MessageWriter;
import java.nio.file.Path;

public interface JsonDescriptorResolver {

    Path jsonForBom(String bomGroupId, String bomArtifactId, String bomVersion, JsonMavenArtifactResolver jsonResolver,
            MessageWriter log) throws Exception;
}
