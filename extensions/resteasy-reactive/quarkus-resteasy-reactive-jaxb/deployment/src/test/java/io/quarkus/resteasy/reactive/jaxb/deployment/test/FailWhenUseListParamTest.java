package io.quarkus.resteasy.reactive.jaxb.deployment.test;

import java.util.List;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class FailWhenUseListParamTest {

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

        @POST
        @Consumes(MediaType.APPLICATION_XML)
        public String hello(List<String> items) {
            return "ok";
        }
    }
}
