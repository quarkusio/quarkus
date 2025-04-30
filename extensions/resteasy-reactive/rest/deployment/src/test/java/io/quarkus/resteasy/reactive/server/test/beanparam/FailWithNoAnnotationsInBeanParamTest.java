package io.quarkus.resteasy.reactive.server.test.beanparam;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.ws.rs.BeanParam;
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

public class FailWithNoAnnotationsInBeanParamTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setExpectedException(DeploymentException.class)
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(GreetingResource.class, NoQueryParamsInFieldsNameData.class));

    @Test
    void shouldBeanParamWorkWithoutFieldsAnnotatedWithQueryParam() {
        Assertions.fail("The test case should not be invoked as it should fail with a deployment exception.");
    }

    @Path("/greeting")
    public static class GreetingResource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String hello(@BeanParam NoQueryParamsInFieldsNameData request) {
            return "Hello, " + request.getName();
        }
    }

    public static class NoQueryParamsInFieldsNameData {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
