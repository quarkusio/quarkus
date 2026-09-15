package io.quarkus.resteasy.reactive.server.test.status;

import static org.hamcrest.Matchers.equalTo;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.ResponseHeader;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;

/**
 * The Jakarta REST annotations live on an interface or an abstract class, while {@link ResponseStatus} and
 * {@link ResponseHeader} are placed on the overriding method of the implementation.
 */
public class ResponseStatusOnOverridingMethodTest {

    @RegisterExtension
    static QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(Api.class, ApiImpl.class, AbstractResource.class,
                    ConcreteResource.class));

    @Test
    public void statusDeclaredOnTheImplementationOnly() {
        RestAssured.get("/api/impl-only").then().statusCode(201).body(equalTo("impl-only"));
    }

    @Test
    public void statusDeclaredOnTheInterfaceOnly() {
        RestAssured.get("/api/interface-only").then().statusCode(202).body(equalTo("interface-only"));
    }

    @Test
    public void implementationStatusWinsOverInterfaceStatus() {
        RestAssured.get("/api/both").then().statusCode(203).body(equalTo("both"));
    }

    @Test
    public void statusDeclaredOnTheImplementationOfAnAsyncMethod() {
        RestAssured.get("/api/uni").then().statusCode(201).body(equalTo("uni"));
    }

    @Test
    public void headerDeclaredOnTheImplementationOnly() {
        RestAssured.get("/api/header").then().statusCode(200).header("X-Impl", "yes").header("X-Impl-2", "also");
    }

    @Test
    public void statusDeclaredOnTheSubclassOfAnAbstractResource() {
        RestAssured.get("/abstract/impl-only").then().statusCode(201).body(equalTo("impl-only"));
    }

    @Test
    public void statusDeclaredOnTheAbstractResourceOnly() {
        RestAssured.get("/abstract/abstract-only").then().statusCode(202).body(equalTo("abstract-only"));
    }

    @Path("/api")
    public interface Api {

        @GET
        @Path("/impl-only")
        String implOnly();

        @ResponseStatus(202)
        @GET
        @Path("/interface-only")
        String interfaceOnly();

        @ResponseStatus(202)
        @GET
        @Path("/both")
        String both();

        @GET
        @Path("/uni")
        Uni<String> uni();

        @GET
        @Path("/header")
        String header();
    }

    public static class ApiImpl implements Api {

        @ResponseStatus(201)
        @Override
        public String implOnly() {
            return "impl-only";
        }

        @Override
        public String interfaceOnly() {
            return "interface-only";
        }

        @ResponseStatus(203)
        @Override
        public String both() {
            return "both";
        }

        @ResponseStatus(201)
        @Override
        public Uni<String> uni() {
            return Uni.createFrom().item("uni");
        }

        @ResponseHeader(name = "X-Impl", value = "yes")
        @ResponseHeader(name = "X-Impl-2", value = "also")
        @Override
        public String header() {
            return "header";
        }
    }

    @Path("/abstract")
    public static abstract class AbstractResource {

        @GET
        @Path("/impl-only")
        public abstract String implOnly();

        @ResponseStatus(202)
        @GET
        @Path("/abstract-only")
        public abstract String abstractOnly();
    }

    public static class ConcreteResource extends AbstractResource {

        @ResponseStatus(201)
        @Override
        public String implOnly() {
            return "impl-only";
        }

        @Override
        public String abstractOnly() {
            return "abstract-only";
        }
    }
}
