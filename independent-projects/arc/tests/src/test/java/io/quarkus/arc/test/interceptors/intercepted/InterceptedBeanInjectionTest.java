package io.quarkus.arc.test.interceptors.intercepted;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.Bean;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class InterceptedBeanInjectionTest {

    @RegisterExtension
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
