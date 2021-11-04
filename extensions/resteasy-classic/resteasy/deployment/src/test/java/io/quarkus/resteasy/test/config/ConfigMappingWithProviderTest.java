package io.quarkus.resteasy.test.config;

import static org.hamcrest.core.Is.is;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

public class ConfigMappingWithProviderTest {
    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MappingResource.class)
                    .addClass(MappingFilter.class)
                    .addClass(Mapping.class)
                    .addAsResource(new StringAsset("mapping.hello=hello\n"), "application.properties"));

    @Test
    void configMapping() {
        RestAssured.when().get("/hello").then()
                .statusCode(200)
                .body(is("hello"));

        TEST.modifyResourceFile("application.properties", s -> "mapping.hello=Hello\n");

        RestAssured.when().get("/hello").then()
                .statusCode(200)
                .body(is("Hello"));
    }

    @Path("/hello")
    public static class MappingResource {
        @GET
        public String hello(@Context HttpRequest request) {
            return (String) request.getAttribute("mapping.hello");
        }
    }

    @Provider
    public static class MappingFilter implements ContainerRequestFilter {
        @Inject
        Mapping mapping;

        @Override
        public void filter(final ContainerRequestContext requestContext) throws IOException {
            requestContext.setProperty("mapping.hello", mapping.hello());
        }
    }

    @StaticInitSafe
    @ConfigMapping(prefix = "mapping")
    public interface Mapping {
        @WithDefault("hello")
        String hello();
    }
}
