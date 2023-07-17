package io.quarkus.arc.test.bean.types;

import static org.assertj.core.api.Assertions.assertThatCollection;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;

public class NonPublicJdkBeanTypesTest {

    @RegisterExtension
    ArcTestContainer container = new ArcTestContainer(StringBuilderProducer.class);

    @Test
    public void test() {
        InstanceHandle<StringBuilder> handle = Arc.container().instance(StringBuilder.class);
        InjectableBean<StringBuilder> bean = handle.getBean();
        assertNotNull(bean);
        assertThatCollection(bean.getTypes())
                .contains(StringBuilder.class, CharSequence.class)
                .noneMatch(t -> t.getTypeName().equals("java.lang.AbstractStringBuilder"));
    }

    @Dependent
    static class StringBuilderProducer {

        @Produces
        StringBuilder produce() {
            return new StringBuilder();
        }
    }
}
