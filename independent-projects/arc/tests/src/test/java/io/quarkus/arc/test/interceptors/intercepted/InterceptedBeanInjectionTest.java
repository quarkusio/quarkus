package io.quarkus.arc.test.interceptors.intercepted;

import static org.junit.Assert.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;
import org.junit.Rule;
import org.junit.Test;

public class InterceptedBeanInjectionTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Simple.class,
            SimpleInterceptor.class, InterceptedBean.class);

    @Test
    public void testInterception() {
        InterceptedBean bean = Arc.container().instance(InterceptedBean.class).get();
        assertEquals(InterceptedBean.class.getName() + InterceptedBean.class.getName(), bean.ping());
    }

    @ApplicationScoped
    static class InterceptedBean {

        @Inject
        Bean<?> bean;

        @Simple
        public String ping() {
            return bean.getBeanClass().getName();
        }

    }

}
