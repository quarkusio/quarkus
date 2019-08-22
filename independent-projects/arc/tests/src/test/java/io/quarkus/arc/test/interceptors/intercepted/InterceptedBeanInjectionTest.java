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
        assertEquals(InterceptedBean.class.getName(), SimpleInterceptor.aroundConstructResult);
        assertEquals(InterceptedBean.class.getName(), SimpleInterceptor.postConstructResult);
    }

    @ApplicationScoped
    @Simple
    static class InterceptedBean {

        @Inject
        Bean<?> bean;

        public String ping() {
            return bean.getBeanClass().getName();
        }

    }

}
