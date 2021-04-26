package io.quarkus.arc.test.interceptors.inheritance.complex;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Heavily inspired by TCK test, see
 * https://github.com/eclipse-ee4j/cdi-tck/blob/3.0.2/impl/src/main/java/org/jboss/cdi/tck/interceptors/tests/contract/aroundInvoke/bindings/AroundInvokeInterceptorTest.java
 * Tests that interceptors can be declared on target class, their super class, interceptors and their superclasses
 * and that the ordering honors what specification requires.
 *
 * @author Matej Novotny
 */
public class ComplexAroundInvokeHierarchyTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Interceptor1.class, Interceptor2.class, Binding.class,
            MiddleInterceptor1.class, MiddleFoo.class, Foo.class, SuperFoo.class, SuperInterceptor2.class,
            SuperInterceptor1.class);

    @Test
    public void testInterceptorMethodHierarchy() throws Exception {
        Foo foo = Arc.container().select(Foo.class).get();
        String result = foo.ping();
        String expected = "ping" + Foo.class.getSimpleName() + MiddleFoo.class.getSimpleName() +
                SuperFoo.class.getSimpleName() + Interceptor2.class.getSimpleName() +
                SuperInterceptor2.class.getSimpleName() + Interceptor1.class.getSimpleName() +
                MiddleInterceptor1.class.getSimpleName() + SuperInterceptor1.class.getSimpleName();
        Assertions.assertEquals(expected, result);
    }
}
