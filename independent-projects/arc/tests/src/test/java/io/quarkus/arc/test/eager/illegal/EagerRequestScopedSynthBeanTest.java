package io.quarkus.arc.test.eager.illegal;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.spi.DefinitionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

public class EagerRequestScopedSynthBeanTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanRegistrars(new BeanRegistrar() {
                @Override
                public void register(RegistrationContext context) {
                    context.configure(MyBean.class)
                            .types(MyBean.class)
                            .scope(RequestScoped.class)
                            .eager(true)
                            .creator(MyBeanCreator.class)
                            .done();
                }
            })
            .shouldFail()
            .build();

    @Test
    public void trigger() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertInstanceOf(DefinitionException.class, error);
        assertTrue(error.getMessage().contains("Synthetic bean declared eager, it must be @ApplicationScoped or @Singleton"));
    }

    static class MyBean {
    }

    static class MyBeanCreator implements BeanCreator<MyBean> {
        @Override
        public MyBean create(SyntheticCreationalContext<MyBean> context) {
            return new MyBean();
        }
    }
}
