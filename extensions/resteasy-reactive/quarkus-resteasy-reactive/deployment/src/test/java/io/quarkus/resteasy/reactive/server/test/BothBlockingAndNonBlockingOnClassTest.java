package io.quarkus.resteasy.reactive.server.test;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.function.Supplier;

import javax.enterprise.inject.spi.DeploymentException;
import javax.ws.rs.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;

public class BothBlockingAndNonBlockingOnClassTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
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
    @Blocking
    @NonBlocking
    public static class Resource {

        @Path("hello")
        public String hello() {
            return "hello";
        }
    }
}
