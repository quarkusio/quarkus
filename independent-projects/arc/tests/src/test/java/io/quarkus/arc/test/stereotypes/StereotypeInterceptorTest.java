package io.quarkus.arc.test.stereotypes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class StereotypeInterceptorTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(BeIntercepted.class, IamIntercepted.class,
            SimpleBinding.class,
            SimpleInterceptor.class);

    @Test
    public void testStereotype() {
        assertEquals("interceptedOK", Arc.container().instance(IamIntercepted.class).get().getId());
    }

    @Test
    public void testStereotypeBeanManager() {
        BeanManager beanManager = CDI.current().getBeanManager();
        Assertions.assertTrue(beanManager.isStereotype(BeIntercepted.class));
        Assertions.assertFalse(beanManager.isStereotype(SimpleBinding.class));

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
