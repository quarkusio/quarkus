package io.quarkus.arc.test.interceptors.intercepted;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class InterceptedBeanInjectionTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Simple.class,
            SimpleInterceptor.class, InterceptedBean.class, InterceptedDependent.class);

    @Test
    public void testInterception() {
        InterceptedBean bean = Arc.container().instance(InterceptedBean.class).get();
        assertEquals(InterceptedBean.class.getName() + InterceptedBean.class.getName(), bean.ping());
        assertEquals(InterceptedBean.class.getName(), SimpleInterceptor.aroundConstructResult);
        assertEquals(InterceptedBean.class.getName(), SimpleInterceptor.postConstructResult);
        assertEquals(
                InterceptedBean.class.getName() + InterceptedDependent.class.getName() + InterceptedDependent.class.getName(),
                bean.pong());
        InterceptedDependent dependent = Arc.container().instance(InterceptedDependent.class).get();
        assertEquals(InterceptedDependent.class.getName() + InterceptedDependent.class.getName(),
                dependent.pong());
    }

    @ApplicationScoped
    @Simple
    static class InterceptedBean {

        @Inject
        Bean<?> bean;

        @Inject
        InterceptedDependent dependent;

        String ping() {
            return bean.getBeanClass().getName();
        }

        String pong() {
            return dependent.pong();
        }

    }

    @Dependent
    static class InterceptedDependent {

        @Simple
        String pong() {
            return InterceptedDependent.class.getName();
        }

    }

}
