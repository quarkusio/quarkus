package io.quarkus.arquillian.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests injection of parameter values into @Test methods.
 */
@ExtendWith(ArquillianExtension.class)
@Disabled
public class MethodParameterInjectionTest {

    @Deployment
    public static JavaArchive createTestArchive() {
        return ShrinkWrap.create(JavaArchive.class).addClasses(AppScopedBean1.class, AppScopedBean2.class);
    }

    @Test
    public void injectOneApplicationScopedBean(AppScopedBean1 param) {
        assertNotNull("Method param was not injected", param);
        assertNotNull("@Inject did not work", injected);
        assertEquals(injected, param);
    }

    @Test
    public void injectTwoApplicationScopedBeans(AppScopedBean1 param1, AppScopedBean2 param2) {
        assertNotNull("Method param was not injected", param1);
        assertNotNull("Method param was not injected", param2);
    }

    @Test
    public void injectFromProducer(OtherBean param1, OtherBean param2) {
        assertNotNull(param1);
        assertNotNull(param2);
        assertNotSame(param1, param2);
    }

    @Test
    public void injectWithQualifier(@Good X param) {
        assertNotNull(param);
        assertTrue(Y.class.isAssignableFrom(param.getClass()));
        assertFalse(Z.class.isAssignableFrom(param.getClass()));
    }

    public interface X {

    }

    @Good
    @ApplicationScoped
    public static class Y implements X {

    }

    @Bad
    @ApplicationScoped
    public static class Z implements X {

    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
    public @interface Good {
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
    public @interface Bad {
    }

    @Inject
    AppScopedBean1 injected;

    @Inject
    AppScopedBean2 injected2;

    @Inject
    @Good
    X yInstance;

    @Inject
    @Bad
    X zInstance;

    @Produces
    @Dependent
    OtherBean producer() {
        return new OtherBean();
    }

    @ApplicationScoped
    public static class AppScopedBean1 {

    }

    @ApplicationScoped
    public static class AppScopedBean2 {

    }

    public static class OtherBean {

    }

}
