package io.quarkus.arc.test.bean.destroy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class DependentPreDestroyOnlyCalledOnceTest {
    @RegisterExtension
    ArcTestContainer container = new ArcTestContainer(MyDependency.class, MyBean.class, MyInterceptedBean.class,
            MyInterceptorBinding.class, MyInterceptor.class);

    @BeforeEach
    public void setUp() {
        MyDependency.preDestroy = 0;
        MyBean.preDestroy = 0;
        MyInterceptedBean.preDestroy = 0;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void preDestroyOnBeanOnly() {
        BeanManager beanManager = Arc.container().beanManager();
        Bean<MyBean> bean = (Bean<MyBean>) beanManager.resolve(beanManager.getBeans(MyBean.class));
        CreationalContext<MyBean> ctx = beanManager.createCreationalContext(bean);
        MyBean instance = (MyBean) beanManager.getReference(bean, MyBean.class, ctx);
        bean.destroy(instance, ctx);

        assertEquals(1, MyDependency.preDestroy);
        assertEquals(1, MyBean.preDestroy);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void preDestroyOnBeanAndInterceptor() {
        BeanManager beanManager = Arc.container().beanManager();
        Bean<MyInterceptedBean> bean = (Bean<MyInterceptedBean>) beanManager.resolve(
                beanManager.getBeans(MyInterceptedBean.class));
        CreationalContext<MyInterceptedBean> ctx = beanManager.createCreationalContext(bean);
        MyInterceptedBean instance = (MyInterceptedBean) beanManager.getReference(bean, MyInterceptedBean.class, ctx);
        bean.destroy(instance, ctx);

        assertEquals(1, MyDependency.preDestroy);
        assertEquals(1, MyInterceptedBean.preDestroy);
        assertEquals(1, MyInterceptor.preDestroy);
    }

    @Dependent
    static class MyDependency {
        static int preDestroy = 0;

        @PreDestroy
        void destroy() {
            preDestroy++;
        }
    }

    @Dependent
    static class MyBean {
        static int preDestroy = 0;

        @Inject
        MyDependency dependency;

        @PreDestroy
        void destroy() {
            preDestroy++;
        }
    }

    @Dependent
    @MyInterceptorBinding
    static class MyInterceptedBean {
        static int preDestroy = 0;

        @Inject
        MyDependency dependency;

        @PreDestroy
        void destroy() {
            preDestroy++;
        }
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
        static int preDestroy = 0;

        @PreDestroy
        void preDestroy(InvocationContext ctx) throws Exception {
            preDestroy++;
            ctx.proceed();
        }
    }
}
