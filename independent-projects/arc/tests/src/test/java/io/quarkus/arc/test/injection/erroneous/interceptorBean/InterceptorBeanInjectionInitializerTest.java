package io.quarkus.arc.test.injection.erroneous.interceptorBean;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.inject.spi.Interceptor;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class InterceptorBeanInjectionInitializerTest {

    @RegisterExtension
    ArcTestContainer container = ArcTestContainer.builder().beanClasses(InterceptorBeanInjectionInitializerTest.class,
            MyBean.class).shouldFail().build();

    @Test
    public void testExceptionThrown() {
        Throwable error = container.getFailure();
        assertThat(error).isInstanceOf(DefinitionException.class);
    }

    @ApplicationScoped
    static class MyBean {

        @Inject
        void initMethod(Interceptor<MyBean> interceptor) {

        }

    }
}
