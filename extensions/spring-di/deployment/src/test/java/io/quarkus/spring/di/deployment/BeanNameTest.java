package io.quarkus.spring.di.deployment;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusExtensionTest;

public class BeanNameTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Bean.class));

    @Inject
    Bean consumer;

    @Test
    public void testBeanName() {
        Assertions.assertSame(consumer, Arc.container().instance("bean").get());
    }
}
