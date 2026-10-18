package io.quarkus.compressors.it;

import jakarta.ws.rs.Path;

@Path("/compressed")
public class SomeCompressedResource extends CompressedResource {
}
