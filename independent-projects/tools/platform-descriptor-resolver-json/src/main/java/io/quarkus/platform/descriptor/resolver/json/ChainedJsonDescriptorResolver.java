package io.quarkus.platform.descriptor.resolver.json;

import io.quarkus.devtools.messagewriter.MessageWriter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class ChainedJsonDescriptorResolver implements JsonDescriptorResolver {

    private final List<JsonDescriptorResolver> chain;

    public ChainedJsonDescriptorResolver(JsonDescriptorResolver... descriptorResolvers) {
        chain = Arrays.asList(descriptorResolvers);
    }

    @Override
    public Path jsonForBom(String bomGroupId, String bomArtifactId, String bomVersion,
            JsonMavenArtifactResolver artifactResolver, MessageWriter log)
            throws Exception {
        for (JsonDescriptorResolver resolver : chain) {
            try {
                final Path p = resolver.jsonForBom(bomGroupId, bomArtifactId, bomVersion, artifactResolver, log);
                if (p != null) {
                    return p;
                }
            } catch (Exception e) {
                if (log != null) {
                    log.debug("Failed to resolve Quarkus platform descriptor for BOM %s:%s:%s: %s", bomGroupId, bomArtifactId,
                            bomVersion, e.getLocalizedMessage());
                }
            }
        }
        throw new Exception(
                "Failed to resolve the JSON descriptor for BOM " + bomGroupId + ":" + bomArtifactId + ":" + bomVersion);
    }
}
