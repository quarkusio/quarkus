package io.quarkus.resteasy.reactive.jaxb.deployment.test;

import java.util.List;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class FailWhenReturnListTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setExpectedException(DeploymentException.class)
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(GreetingResource.class));

    @Test
    void shouldFailWithDeploymentException() {
        Assertions.fail("The test case should not be invoked as it should fail with a deployment exception.");
    }

    @Path("/greeting")
    public static class GreetingResource {

        @GET
        @Produces(MediaType.APPLICATION_XML)
        public List<String> hello() {
            return List.of("1", "2", "3");
        }
    }
}
