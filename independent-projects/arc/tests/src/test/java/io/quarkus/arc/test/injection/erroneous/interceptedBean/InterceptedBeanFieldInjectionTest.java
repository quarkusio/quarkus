package io.quarkus.arc.test.injection.erroneous.interceptedBean;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Intercepted;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class InterceptedBeanFieldInjectionTest {

    @RegisterExtension
    ArcTestContainer container = ArcTestContainer.builder().beanClasses(InterceptedBeanFieldInjectionTest.class,
            MyBean.class).shouldFail().build();

    @Test
    public void testExceptionThrown() {
        Throwable error = container.getFailure();
        assertThat(error).isInstanceOf(DefinitionException.class);
    }

    @ApplicationScoped
    static class MyBean {

        @Inject
        @Intercepted
        Bean<MyBean> bean;
    }
}
