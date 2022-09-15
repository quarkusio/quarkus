package io.quarkus.arc.test.interceptors.nonpublicparams;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.interceptors.Simple;
import io.quarkus.arc.test.interceptors.nonpublicparams.charlie.Charlie;
import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class NonPublicParametersTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Simple.class, SimpleBean.class,
            SimpleInterceptor.class);

    @Test
    public void testInterception() {
        ArcContainer arc = Arc.container();
        SimpleBean simpleBean = arc.instance(SimpleBean.class).get();
        assertEquals("FOO",
                simpleBean.foo(null, new Bar(), new Baz(), null));
    }

    @Singleton
    static class SimpleBean extends Charlie {

        @Simple
        String foo(Foo foo, Bar bar, Baz baz, CharlieParam charlie) {
            return "foo";
        }

        @Simple
        String fooArray(Foo[] foo, boolean isOk) {
            return "foo";
        }

        @Simple
        String primitiveArray(int[] ints) {
            return "foo";
        }

        private static class Foo {
        }

    }

    static class Bar {

    }

    protected static class Baz {

    }

    @Simple
    @Priority(1)
    @Interceptor
    public static class SimpleInterceptor {

        @AroundInvoke
        Object mySuperCoolAroundInvoke(InvocationContext ctx) throws Exception {
            return ctx.proceed().toString().toUpperCase();
        }
    }

}
