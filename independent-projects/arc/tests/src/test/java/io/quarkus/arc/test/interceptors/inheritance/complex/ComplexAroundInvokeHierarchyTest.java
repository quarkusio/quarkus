package io.quarkus.arc.test.interceptors.inheritance.complex;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
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

    public static String aroundConstructVal = "";
    public static String postConstructVal = "";
    public static String preDestroyVal = "";

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Interceptor1.class, Interceptor2.class, Binding.class,
            MiddleInterceptor1.class, Foo.class, SuperInterceptor2.class,
            SuperInterceptor1.class);

    @Test
    public void testInterceptorMethodHierarchy() throws Exception {
        InjectableInstance<Foo> beanInstance = Arc.container().select(Foo.class);
        Foo foo = beanInstance.get();
        String result = foo.ping();
        String expected = "pong" + Interceptor2.class.getSimpleName() +
                Interceptor1.class.getSimpleName() +
                MiddleInterceptor1.class.getSimpleName() + SuperInterceptor1.class.getSimpleName();
        String otherExpected = Interceptor2.class.getSimpleName() +
                SuperInterceptor2.class.getSimpleName() + Interceptor1.class.getSimpleName() +
                MiddleInterceptor1.class.getSimpleName() + SuperInterceptor1.class.getSimpleName();
        beanInstance.getHandle().destroy();
        Assertions.assertEquals(expected, result);
        Assertions.assertEquals(otherExpected, aroundConstructVal);
        Assertions.assertEquals(otherExpected, postConstructVal);
        Assertions.assertEquals(otherExpected, preDestroyVal);
    }
}
