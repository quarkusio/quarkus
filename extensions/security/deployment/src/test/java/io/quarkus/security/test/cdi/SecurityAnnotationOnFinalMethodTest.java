package io.quarkus.security.test.cdi;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class SecurityAnnotationOnFinalMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(SomeBean.class))
            .setExpectedException(DeploymentException.class);

    @Inject
    SomeBean simpleBean;

    @Test
    public void test() {
        // should not be called, deployment exception should happen first.
        Assertions.fail();
    }

    @Singleton
    public static class SomeBean {

        @RolesAllowed("admin")
        public final void someMethod() {

        }
    }
}
