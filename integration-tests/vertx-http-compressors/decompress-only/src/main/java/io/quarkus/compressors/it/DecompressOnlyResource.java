package io.quarkus.compressors.it;

import jakarta.ws.rs.Path;

@Path("/decompressed")
public class DecompressOnlyResource extends DecompressResource {
}
