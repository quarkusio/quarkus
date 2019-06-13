package io.quarkus.arc.test.inheritance;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class ScopeInheritanceTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(SuperBean.class, SubBean.class, RequestScopedSubBean.class,
            InheritedScopeSubBean.class, JustSomeBeanDefiningAnnotation.class);

    @Test
    public void testScopeInheritanceIsTakenIntoAccount() {
        // we'll use BM to verify scopes
        BeanManager bm = Arc.container().beanManager();
        Set<Bean<?>> beans = bm.getBeans(RequestScopedSubBean.class);
        Assert.assertTrue(beans.size() == 1);
        Assert.assertEquals(RequestScoped.class.getSimpleName(), beans.iterator().next().getScope().getSimpleName());
        beans = bm.getBeans(InheritedScopeSubBean.class);
        Assert.assertTrue(beans.size() == 1);
        Assert.assertEquals(ApplicationScoped.class.getSimpleName(), beans.iterator().next().getScope().getSimpleName());
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
