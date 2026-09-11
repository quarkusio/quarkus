package org.acme.application;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.acme.extension.runtime.IncludedExtensionRuntime;

@Path("/hello")
public class GreetingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() throws IOException {
        try (InputStream marker = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("included-extension-deployment.txt")) {
            if (marker == null) {
                return IncludedExtensionRuntime.message() + " / deployment marker missing";
            }
            return IncludedExtensionRuntime.message() + " / "
                    + new String(marker.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
