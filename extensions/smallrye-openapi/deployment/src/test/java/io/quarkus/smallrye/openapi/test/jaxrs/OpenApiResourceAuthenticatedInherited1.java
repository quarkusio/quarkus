package io.quarkus.smallrye.openapi.test.jaxrs;

import jakarta.ws.rs.Path;

import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.quarkus.security.Authenticated;

@Path("/resource-inherited1")
@Tag(name = "test")
@Server(url = "serverUrl")
@Authenticated
public class OpenApiResourceAuthenticatedInherited1 extends OpenApiResourceAuthenticatedAtClassLevel {
}
