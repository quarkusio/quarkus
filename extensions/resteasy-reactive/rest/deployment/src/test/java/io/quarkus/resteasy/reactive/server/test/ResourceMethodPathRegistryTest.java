package io.quarkus.resteasy.reactive.server.test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.UriBuilder;

import org.jboss.resteasy.reactive.common.jaxrs.ResourceMethodPathRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class ResourceMethodPathRegistryTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(MyResource.class));

    @Test
    public void registryIsPopulatedFromBuildTimeMetadata() {
        // proves the build step recorded the raw @Path value for (MyResource, "list") with no runtime reflection
        when().get("/uritest/registry-path").then().statusCode(200).body(equalTo("/list"));
    }

    @Test
    public void uriBuilderResolvesMethod() {
        when().get("/uritest/resolved").then().statusCode(200).body(equalTo("/list"));
    }

    @Test
    public void registryHasNoEntryForUnknownClass() {
        when().get("/uritest/registry-unknown").then().statusCode(200).body(equalTo("null"));
    }

    @Path("/uritest")
    public static class MyResource {

        @GET
        @Path("/list")
        public String list() {
            return "list";
        }

        @GET
        @Path("/registry-path")
        public String registryPath() {
            return String.valueOf(ResourceMethodPathRegistry.getPath(MyResource.class.getName(), "list"));
        }

        @GET
        @Path("/registry-unknown")
        public String registryUnknown() {
            return String.valueOf(ResourceMethodPathRegistry.getPath("does.not.Exist", "list"));
        }

        @GET
        @Path("/resolved")
        public String resolved() {
            return UriBuilder.newInstance().path(MyResource.class, "list").toTemplate();
        }
    }
}
