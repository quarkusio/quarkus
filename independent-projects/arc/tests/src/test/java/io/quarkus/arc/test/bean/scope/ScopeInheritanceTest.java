package io.quarkus.arc.test.bean.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.NormalScope;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class ScopeInheritanceTest {
    @RegisterExtension
    ArcTestContainer container = new ArcTestContainer(MyScope.class, MyStereotype.class, MyBean1.class, MyBean2.class);

    @Test
    public void nonInheritedScopeOnDirectSuperclass() {
        BeanManager bm = Arc.container().beanManager();
        Bean<MyBean1> bean = (Bean<MyBean1>) bm.resolve(bm.getBeans(MyBean1.class));
        assertEquals(Dependent.class, bean.getScope());
    }

    @Test
    public void inheritedScopeOnDirectSuperclass() {
        BeanManager bm = Arc.container().beanManager();
        Bean<MyBean2> bean = (Bean<MyBean2>) bm.resolve(bm.getBeans(MyBean2.class));
        assertEquals(RequestScoped.class, bean.getScope());
    }

    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    @NormalScope
    @interface MyScope {
    }

    // just a bean defining annotation
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    @Stereotype
    @interface MyStereotype {
    }

    @MyStereotype
    static class MyBean1 extends DirectSuperclassWithNonInheritedScope {
    }

    @MyStereotype
    static class MyBean2 extends DirectSuperclassWithInheritedScope {
    }

    // `MyScope` is not `@Inherited`, so `MyBean1` will not inherit it, but it will
    // also prevent inheriting `@ApplicationScoped` from `IndirectSuperclass`
    @MyScope
    static class DirectSuperclassWithNonInheritedScope extends IndirectSuperclass {
    }

    @RequestScoped
    static class DirectSuperclassWithInheritedScope extends IndirectSuperclass {
    }

    @ApplicationScoped
    static class IndirectSuperclass {
    }
}
