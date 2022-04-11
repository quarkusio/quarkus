package org.jboss.resteasy.reactive.server.vertx.test;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.function.Supplier;
import javax.enterprise.inject.spi.DeploymentException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class MultipleHttpAnnotationsTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Resource.class);
                }
            }).setExpectedException(DeploymentException.class);

    @Test
    public void test() {
        fail("Should never have been called");
    }

    @Path("test")
    public static class Resource {

        @Path("hello")
        @GET
        @POST
        public String hello() {
            return "hello";
        }
    }
}
