package io.quarkus.it.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.it.arc.UnusedBean;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class BeanOnlyInTestCase {

    @Inject
    UnusedBean unusedBean;

    @Test
    public void testBeanIsInjected() {
        assertNotNull(unusedBean);
        InjectionPoint injectionPoint = unusedBean.getInjectionPoint();
        assertNotNull(injectionPoint);
        assertEquals(UnusedBean.class, injectionPoint.getType());
        assertTrue(injectionPoint.getQualifiers().isEmpty());
        assertTrue(injectionPoint.getMember() instanceof Field);
        assertTrue(injectionPoint.getAnnotated().isAnnotationPresent(Inject.class));
    }
}
