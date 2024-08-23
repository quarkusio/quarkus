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
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Stereotype;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.test.ArcTestContainer;

public class InconsistentPriorityStereotypesOverriddenOnBeanTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Stereotype1.class, Stereotype2.class, Stereotype3.class,
            MyBean.class);

    @Test
    public void test() {
        InjectableBean<MyBean> bean = Arc.container().instance(MyBean.class).getBean();
        assertTrue(bean.isAlternative());
        assertEquals(789, bean.getPriority());
    }

    // stereotype transitivity:
    // 1 --> 2, 3

    @Alternative
    @Stereotype2
    @Stereotype3
    @Stereotype
    @Target({ TYPE, METHOD, FIELD })
    @Retention(RUNTIME)
    @interface Stereotype1 {
    }

    @Priority(123)
    @Stereotype
    @Target({ TYPE, METHOD, FIELD })
    @Retention(RUNTIME)
    @interface Stereotype2 {
    }

    @Priority(456)
    @Stereotype
    @Target({ TYPE, METHOD, FIELD })
    @Retention(RUNTIME)
    @interface Stereotype3 {
    }

    @Dependent
    @Stereotype1
    @Priority(789)
    static class MyBean {
    }
}
