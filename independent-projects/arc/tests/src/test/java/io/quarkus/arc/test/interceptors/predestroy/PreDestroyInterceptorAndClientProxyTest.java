package io.quarkus.arc.test.interceptors.predestroy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.test.ArcTestContainer;

public class PreDestroyInterceptorAndClientProxyTest {
    @RegisterExtension
    ArcTestContainer container = new ArcTestContainer(MyBean.class, MyInterceptorBinding.class, MyInterceptor.class);

    @Test
    public void test() {
        BeanManager beanManager = Arc.container().beanManager();

        Bean<MyBean> bean = (Bean<MyBean>) beanManager.resolve(beanManager.getBeans(MyBean.class));
        CreationalContext<MyBean> ctx = beanManager.createCreationalContext(bean);

        MyBean instance = (MyBean) beanManager.getReference(bean, MyBean.class, ctx);
        assertNotNull(instance);
        assertInstanceOf(ClientProxy.class, instance);

        assertFalse(MyInterceptor.intercepted);
        bean.destroy(instance, ctx);
        assertTrue(MyInterceptor.intercepted);
    }

    @ApplicationScoped
    @MyInterceptorBinding
    static class MyBean {
    }

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @InterceptorBinding
    @interface MyInterceptorBinding {
    }

    @MyInterceptorBinding
    @Interceptor
    @Priority(1)
    static class MyInterceptor {
        static boolean intercepted = false;

        @PreDestroy
        void intercept(InvocationContext ctx) throws Exception {
            intercepted = true;
            ctx.proceed();
        }
    }
}
