package io.quarkus.registry;

import java.io.IOException;
import java.io.StringWriter;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkus.registry.config.RegistriesConfigMapperHelper;
import io.quarkus.registry.config.RegistryConfig;

@Path("/config")
public class RegistryConfigResource {

    @GET
    public Response getDefaultRegistryConfig() throws IOException {
        StringWriter sw = new StringWriter();
        RegistriesConfigMapperHelper.toJson(RegistryConfig.defaultConfig(), sw);
        return Response.ok(sw.toString())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .build();
    }
}
