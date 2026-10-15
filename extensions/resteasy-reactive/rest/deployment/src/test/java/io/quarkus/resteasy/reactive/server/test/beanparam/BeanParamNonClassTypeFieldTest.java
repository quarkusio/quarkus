package io.quarkus.resteasy.reactive.server.test.beanparam;

import java.util.List;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestForm;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class BeanParamNonClassTypeFieldTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(Item.class, Container.class, Resource.class))
            .assertException(t -> {
                Throwable root = t;
                while (root.getCause() != null && root.getCause() != root) {
                    root = root.getCause();
                }
                if (root.getMessage() != null && root.getMessage().contains("Not a class type!")) {
                    throw new AssertionError("Deployment failed with an internal Jandex error", root);
                }
            });

    @Test
    public void deploymentDoesNotFailWithInternalError() {
        Assertions.fail();
    }

    public static class Item {

        @RestForm
        String name;
    }

    public static class Container {

        @BeanParam
        List<Item> items;
    }

    @Path("/container")
    public static class Resource {

        @POST
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        @Produces(MediaType.TEXT_PLAIN)
        public String post(@BeanParam Container container) {
            return "ok";
        }
    }
}
