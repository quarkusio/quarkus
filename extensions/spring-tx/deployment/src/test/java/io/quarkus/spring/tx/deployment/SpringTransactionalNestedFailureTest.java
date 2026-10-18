package io.quarkus.spring.tx.deployment;

import static org.junit.jupiter.api.Assertions.fail;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.quarkus.test.QuarkusExtensionTest;

public class SpringTransactionalNestedFailureTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class).addClass(NestedBean.class))
            .setExpectedException(IllegalArgumentException.class);

    @Test
    public void testNestedPropagationShouldFail() {
        fail("Application should not start when propagation NESTED is used");
    }

    @ApplicationScoped
    static class NestedBean {

        @Transactional(propagation = Propagation.NESTED)
        public void nestedMethod() {
        }
    }
}
