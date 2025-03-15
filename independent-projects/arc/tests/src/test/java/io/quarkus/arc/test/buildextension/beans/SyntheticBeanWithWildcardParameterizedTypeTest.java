package io.quarkus.arc.test.buildextension.beans;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jakarta.enterprise.inject.spi.DefinitionException;

import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.WildcardType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

public class SyntheticBeanWithWildcardParameterizedTypeTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanRegistrars(new MyBeanRegistrar())
            .shouldFail()
            .build();

    @Test
    public void trigger() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertInstanceOf(DefinitionException.class, error);
        assertTrue(error.getMessage().contains("Wildcard type is not a legal bean type"));
    }

    static class MyBean {
    }

    static class MyBeanCreator implements BeanCreator<MyBean> {
        @Override
        public MyBean create(SyntheticCreationalContext<MyBean> context) {
            return new MyBean();
        }
    }

    static class MyBeanRegistrar implements BeanRegistrar {
        @Override
        public void register(RegistrationContext context) {
            context.configure(MyBean.class)
                    .addType(MyBean.class)
                    .addType(ParameterizedType.builder(List.class).addArgument(WildcardType.UNBOUNDED).build())
                    .creator(MyBeanCreator.class)
                    .done();
        }
    }
}
