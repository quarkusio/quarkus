package org.jboss.protean.arc.test.stereotypes;

import static org.junit.Assert.assertEquals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.inject.Stereotype;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class StereotypeInterceptorTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(BeIntercepted.class, IamIntercepted.class, SimpleBinding.class, SimpleInterceptor.class);

    @Test
    public void testStereotype() {
        assertEquals("interceptedOK", Arc.container().instance(IamIntercepted.class).get().getId());
    }

    @SimpleBinding
    @Documented
    @Stereotype
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface BeIntercepted {
    }

    @BeIntercepted
    static class IamIntercepted {

        public String getId() {
            return "OK";
        }

    }

}
