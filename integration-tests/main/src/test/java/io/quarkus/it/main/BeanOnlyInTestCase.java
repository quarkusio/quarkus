package io.quarkus.it.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.it.arc.UnusedBean;
import io.quarkus.test.junit.QuarkusTestExtension;

@ExtendWith(QuarkusTestExtension.class)
class BeanOnlyInTestCase {

    @Inject
    UnusedBean unusedBean;

    @Test
    public void testBeanIsInjected() {
        assertNotNull(unusedBean);
        InjectionPoint injectionPoint = unusedBean.getInjectionPoint();
        assertNotNull(injectionPoint);
        assertEquals(UnusedBean.class, injectionPoint.getType());
        assertEquals(1, injectionPoint.getQualifiers().size());
        assertEquals(Default.class, injectionPoint.getQualifiers().iterator().next().annotationType());
        assertTrue(injectionPoint.getMember() instanceof Field);
        assertTrue(injectionPoint.getAnnotated().isAnnotationPresent(Inject.class));
    }
}
