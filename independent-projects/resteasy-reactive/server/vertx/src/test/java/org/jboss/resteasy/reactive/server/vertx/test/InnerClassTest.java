package org.jboss.resteasy.reactive.server.vertx.test;

import static org.junit.jupiter.api.Assertions.fail;

import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.ws.rs.Path;
import java.util.function.Supplier;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class InnerClassTest {

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
    public class Resource {

        @Path("hello")
        @Blocking
        public String hello() {
            return "hello";
        }
    }
}
