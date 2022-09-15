package io.quarkus.arc.test.interceptors.inheritance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * "Around-invoke methods may be defined on interceptor classes and/or the target class and/or super-
 * classes of the target class or the interceptor classes. However, only one around-invoke method may be
 * defined on a given class."
 */
public class InterceptorSuperclassTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Interceptor1.class, Interceptor2.class, One.class, Two.class,
            OverridenInterceptor.class, Fool.class);

    @Test
    public void testInterception() {
        Fool fool = Arc.container().instance(Fool.class).get();
        assertEquals("ping1", fool.pingOne());
        assertEquals("pingoverriden", fool.pingTwo());
    }

    @ApplicationScoped
    public static class Fool {

        @One
        String pingOne() {
            return "ping";
        }

        @Two
        String pingTwo() {
            return "ping";
        }

    }

}
