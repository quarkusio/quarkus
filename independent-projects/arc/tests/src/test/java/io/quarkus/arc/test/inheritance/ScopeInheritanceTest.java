package io.quarkus.arc.test.inheritance;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ScopeInheritanceTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(SuperBean.class, SubBean.class,
            RequestScopedSubBean.class,
            InheritedScopeSubBean.class, JustSomeBeanDefiningAnnotation.class);

    @Test
    public void testScopeInheritanceIsTakenIntoAccount() {
        // we'll use BM to verify scopes
        BeanManager bm = Arc.container().beanManager();
        Set<Bean<?>> beans = bm.getBeans(RequestScopedSubBean.class);
        Assertions.assertTrue(beans.size() == 1);
        Assertions.assertEquals(RequestScoped.class.getSimpleName(), beans.iterator().next().getScope().getSimpleName());
        beans = bm.getBeans(InheritedScopeSubBean.class);
        Assertions.assertTrue(beans.size() == 1);
        Assertions.assertEquals(ApplicationScoped.class.getSimpleName(), beans.iterator().next().getScope().getSimpleName());
    }

    @ApplicationScoped
    static class SuperBean {

        public void ping() {
        }
    }

    // should inherit dependent
    static class SubBean extends SuperBean {

    }

    @RequestScoped
    // declares explicit scope, no inheritance
    static class RequestScopedSubBean extends SubBean {

    }

    // should inherit scope
    @JustSomeBeanDefiningAnnotation // just to add bean defining annotation to have it recognized
    static class InheritedScopeSubBean extends SubBean {

    }

    @Stereotype
    @Inherited
    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @interface JustSomeBeanDefiningAnnotation {

    }
}
