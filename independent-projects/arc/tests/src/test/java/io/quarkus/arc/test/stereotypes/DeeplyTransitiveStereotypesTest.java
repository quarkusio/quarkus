package io.quarkus.arc.test.stereotypes;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Stereotype;
import jakarta.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.test.ArcTestContainer;

public class DeeplyTransitiveStereotypesTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(
            Stereotype1.class,
            Stereotype2.class,
            Stereotype3.class,
            Stereotype4.class,
            Stereotype5.class,
            Stereotype6.class,
            Stereotype7.class,
            MyBean.class);

    @Test
    public void test() {
        InjectableBean<MyBean> bean = Arc.container().instance(MyBean.class).getBean();
        assertEquals(RequestScoped.class, bean.getScope());
        assertEquals("myBean", bean.getName());
        assertTrue(bean.isAlternative());
        assertEquals(123, bean.getPriority());
    }

    // stereotype transitivity:
    // 1 --> 2
    // 2 --> 3
    // 3 --> 4, 5
    // 4 --> 6
    // 5 --> 7

    @Stereotype2
    @Stereotype
    @Target({ TYPE, METHOD, FIELD })
    @Retention(RUNTIME)
    @interface Stereotype1 {
    }

    @Stereotype3
    @Stereotype
    @Target({ TYPE, METHOD, FIELD })
    @Retention(RUNTIME)
    @interface Stereotype2 {
    }

    @Stereotype4
    @Stereotype5
    @Stereotype
    @Target({ TYPE, METHOD, FIELD })
    @Retention(RUNTIME)
    @interface Stereotype3 {
    }

    @RequestScoped
    @Stereotype6
    @Stereotype
    @Target({ TYPE, METHOD, FIELD })
    @Retention(RUNTIME)
    @interface Stereotype4 {
    }

    @Named
    @Stereotype7
    @Stereotype
    @Target({ TYPE, METHOD, FIELD })
    @Retention(RUNTIME)
    @interface Stereotype5 {
    }

    @Alternative
    @Stereotype
    @Target({ TYPE, METHOD, FIELD })
    @Retention(RUNTIME)
    @interface Stereotype6 {
    }

    @Priority(123)
    @Stereotype
    @Target({ TYPE, METHOD, FIELD })
    @Retention(RUNTIME)
    @interface Stereotype7 {
    }

    @Stereotype1
    static class MyBean {
    }
}
