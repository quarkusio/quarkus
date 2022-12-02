package io.quarkus.arc.test.interceptors.noclassinterceptors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

/**
 * Tests that method annotated @NoClassInterceptors will not be intercepted by class-level interceptors.
 */
public class NoClassInterceptorsTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(ClassLevel.class, ClassLevelInterceptor.class,
            InheritedClassLevel.class, InheritedClassLevelInterceptor.class,
            MethodLevel.class, MethodLevelInterceptor.class,
            SuperclassWithInterceptor.class, InterceptedBean.class);

    @Test
    public void testInterception() {
        assertEquals(0, ClassLevelInterceptor.AROUND_CONSTRUCT_COUNTER);
        assertEquals(0, InheritedClassLevelInterceptor.AROUND_CONSTRUCT_COUNTER);
        assertEquals(0, MethodLevelInterceptor.AROUND_CONSTRUCT_COUNTER);

        InterceptedBean bean = Arc.container().instance(InterceptedBean.class).get();
        bean.toString(); // force bean instantiation

        assertEquals(0, ClassLevelInterceptor.AROUND_CONSTRUCT_COUNTER);
        assertEquals(0, InheritedClassLevelInterceptor.AROUND_CONSTRUCT_COUNTER);
        assertEquals(1, MethodLevelInterceptor.AROUND_CONSTRUCT_COUNTER);

        // ---

        assertEquals(0, ClassLevelInterceptor.AROUND_INVOKE_COUNTER);
        assertEquals(0, InheritedClassLevelInterceptor.AROUND_INVOKE_COUNTER);
        assertEquals(0, MethodLevelInterceptor.AROUND_INVOKE_COUNTER);

        bean.inheritedClassLevel();

        assertEquals(1, ClassLevelInterceptor.AROUND_INVOKE_COUNTER);
        assertEquals(1, InheritedClassLevelInterceptor.AROUND_INVOKE_COUNTER);
        assertEquals(0, MethodLevelInterceptor.AROUND_INVOKE_COUNTER);

        bean.inheritedClassLevelAndMethodLevel();

        assertEquals(2, ClassLevelInterceptor.AROUND_INVOKE_COUNTER);
        assertEquals(2, InheritedClassLevelInterceptor.AROUND_INVOKE_COUNTER);
        assertEquals(1, MethodLevelInterceptor.AROUND_INVOKE_COUNTER);

        bean.inheritedMethodLevelOnly();

        assertEquals(2, ClassLevelInterceptor.AROUND_INVOKE_COUNTER);
        assertEquals(2, InheritedClassLevelInterceptor.AROUND_INVOKE_COUNTER);
        assertEquals(2, MethodLevelInterceptor.AROUND_INVOKE_COUNTER);

        bean.inheritedNoInterceptors();

        assertEquals(2, ClassLevelInterceptor.AROUND_INVOKE_COUNTER);
        assertEquals(2, InheritedClassLevelInterceptor.AROUND_INVOKE_COUNTER);
        assertEquals(2, MethodLevelInterceptor.AROUND_INVOKE_COUNTER);

        bean.classLevel();

        assertEquals(3, ClassLevelInterceptor.AROUND_INVOKE_COUNTER);
        assertEquals(3, InheritedClassLevelInterceptor.AROUND_INVOKE_COUNTER);
        assertEquals(2, MethodLevelInterceptor.AROUND_INVOKE_COUNTER);

        bean.classLevelAndMethodLevel();

        assertEquals(4, ClassLevelInterceptor.AROUND_INVOKE_COUNTER);
        assertEquals(4, InheritedClassLevelInterceptor.AROUND_INVOKE_COUNTER);
        assertEquals(3, MethodLevelInterceptor.AROUND_INVOKE_COUNTER);

        bean.methodLevelOnly();

        assertEquals(4, ClassLevelInterceptor.AROUND_INVOKE_COUNTER);
        assertEquals(4, InheritedClassLevelInterceptor.AROUND_INVOKE_COUNTER);
        assertEquals(4, MethodLevelInterceptor.AROUND_INVOKE_COUNTER);

        bean.noInterceptors();

        assertEquals(4, ClassLevelInterceptor.AROUND_INVOKE_COUNTER);
        assertEquals(4, InheritedClassLevelInterceptor.AROUND_INVOKE_COUNTER);
        assertEquals(4, MethodLevelInterceptor.AROUND_INVOKE_COUNTER);

        bean.defaultMethod();

        assertEquals(4, ClassLevelInterceptor.AROUND_INVOKE_COUNTER);
        assertEquals(4, InheritedClassLevelInterceptor.AROUND_INVOKE_COUNTER);
        assertEquals(4, MethodLevelInterceptor.AROUND_INVOKE_COUNTER);

        bean.inheritedDefaultMethod();

        assertEquals(5, ClassLevelInterceptor.AROUND_INVOKE_COUNTER);
        assertEquals(5, InheritedClassLevelInterceptor.AROUND_INVOKE_COUNTER);
        assertEquals(4, MethodLevelInterceptor.AROUND_INVOKE_COUNTER);
    }
}
