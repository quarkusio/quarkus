package io.quarkus.resteasy.reactive.server.test;

import static org.hamcrest.Matchers.equalTo;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Uni;

/**
 * The Jakarta REST annotations live on an abstract class (the shape produced by OpenAPI generated stubs) and
 * {@link Blocking} / {@link NonBlocking} are placed on the overriding method of the concrete subclass.
 */
public class ExecutionModelOnSubclassOfAbstractResourceTest {

    @RegisterExtension
    static QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(AbstractResource.class, ConcreteResource.class,
                    BaseResource.class, MiddleResource.class, DeepResource.class));

    @Test
    public void blockingDeclaredOnTheSubclass() {
        RestAssured.get("/abstract/blocking").then().statusCode(200).body(equalTo("blocking allowed: true"));
    }

    @Test
    public void nonBlockingDeclaredOnTheSubclass() {
        RestAssured.get("/abstract/non-blocking").then().statusCode(200).body(equalTo("blocking allowed: false"));
    }

    @Test
    public void defaultsAreUnchangedWithoutAnAnnotationOnTheSubclass() {
        RestAssured.get("/abstract/default-uni").then().statusCode(200).body(equalTo("blocking allowed: false"));
        RestAssured.get("/abstract/default-plain").then().statusCode(200).body(equalTo("blocking allowed: true"));
    }

    @Test
    public void blockingDeclaredTwoLevelsBelowTheAbstractResource() {
        RestAssured.get("/deep/blocking").then().statusCode(200).body(equalTo("blocking allowed: true"));
    }

    @Path("/abstract")
    public static abstract class AbstractResource {

        @GET
        @Path("/blocking")
        public abstract Uni<String> blocking();

        @GET
        @Path("/non-blocking")
        public abstract String nonBlocking();

        @GET
        @Path("/default-uni")
        public abstract Uni<String> defaultUni();

        @GET
        @Path("/default-plain")
        public abstract String defaultPlain();
    }

    public static class ConcreteResource extends AbstractResource {

        @Blocking
        @Override
        public Uni<String> blocking() {
            return Uni.createFrom().item(describe());
        }

        @NonBlocking
        @Override
        public String nonBlocking() {
            return describe();
        }

        @Override
        public Uni<String> defaultUni() {
            return Uni.createFrom().item(describe());
        }

        @Override
        public String defaultPlain() {
            return describe();
        }
    }

    @Path("/deep")
    public static abstract class BaseResource {

        @GET
        @Path("/blocking")
        public abstract Uni<String> blocking();
    }

    public static abstract class MiddleResource extends BaseResource {
    }

    public static class DeepResource extends MiddleResource {

        @Blocking
        @Override
        public Uni<String> blocking() {
            return Uni.createFrom().item(describe());
        }
    }

    static String describe() {
        return "blocking allowed: " + BlockingOperationControl.isBlockingAllowed();
    }
}
