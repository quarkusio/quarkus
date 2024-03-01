package io.quarkus.resteasy.reactive.server.test.providers;

import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.Providers;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.config.common.MapBackedConfigSource;

public class ProviderConfigInjectionTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().withApplicationRoot(
            jar -> jar.addAsServiceProvider("org.eclipse.microprofile.config.spi.ConfigSource",
                    StaticConfigSource.class.getName(), RuntimeConfigSource.class.getName()));

    @Test
    public void configInjection() {
        RestAssured.when().get("/test").then().body(Matchers.is("runtime"));
    }

    @Path("/test")
    public static class TestResource {
        @Context
        Providers providers;

        @GET
        public String getFoo() {
            return providers.getContextResolver(String.class, MediaType.TEXT_PLAIN_TYPE).getContext(null);
        }
    }

    @Provider
    public static class FooProvider implements ContextResolver<String> {
        @ConfigProperty(name = "configProperty", defaultValue = "configProperty")
        String configProperty;

        @Override
        public String getContext(Class<?> type) {
            return configProperty;
        }
    }

    @StaticInitSafe
    public static class StaticConfigSource extends MapBackedConfigSource {
        public StaticConfigSource() {
            super("static", Map.of("configProperty", "static"), 100);
        }
    }

    public static class RuntimeConfigSource extends MapBackedConfigSource {
        public RuntimeConfigSource() {
            super("runtime", Map.of("configProperty", "runtime"), 200);
        }
    }
}
