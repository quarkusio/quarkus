package io.quarkus.arc.test.bean.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.context.NormalScope;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Scope;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class CustomScopeWithoutContextTest {
    @RegisterExtension
    ArcTestContainer container = new ArcTestContainer(MyScope.class, MyNormalScope.class, MyBean.class, MyNormalBean.class);

    @Test
    public void pseudoScope() {
        BeanManager bm = Arc.container().beanManager();
        Bean<MyBean> bean = (Bean<MyBean>) bm.resolve(bm.getBeans(MyBean.class));
        assertEquals(MyScope.class, bean.getScope());
    }

    @Test
    public void normalScope() {
        BeanManager bm = Arc.container().beanManager();
        Bean<MyNormalBean> bean = (Bean<MyNormalBean>) bm.resolve(bm.getBeans(MyNormalBean.class));
        assertEquals(MyNormalScope.class, bean.getScope());
    }

    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    @Scope
    @interface MyScope {
    }

    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    @NormalScope
    @interface MyNormalScope {
    }

    @MyScope
    static class MyBean {
    }

    @MyNormalScope
    static class MyNormalBean {
    }
}
