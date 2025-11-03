package io.quarkus.arc.test.unused;

import jakarta.enterprise.context.Dependent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class RemoveUnusedSingleBeanTest extends RemoveUnusedComponentsTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyBean.class)
            .removeUnusedBeans(true)
            .build();

    @Test
    public void test() {
        assertNotPresent(MyBean.class);
    }

    @Dependent
    static class MyBean {
    }
}
