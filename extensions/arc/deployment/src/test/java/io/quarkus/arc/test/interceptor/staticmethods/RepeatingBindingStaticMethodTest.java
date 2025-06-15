package io.quarkus.arc.test.interceptor.staticmethods;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class RepeatingBindingStaticMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withApplicationRoot(root -> root
            .addClasses(InterceptMe.class, SimpleBean.class, InterceptMeAlpha.class, InterceptMeBravo.class));

    @Test
    public void testInterceptor() {
        assertEquals("a/b/PONG/b/a", SimpleBean.ping("pong"));
    }

    public static class SimpleBean {

        @InterceptMe("alpha")
        @InterceptMe("bravo")
        public static String ping(String val) {
            return val.toUpperCase();
        }
    }

    @Priority(1)
    @Interceptor
    @InterceptMe("alpha")
    static class InterceptMeAlpha {

        @AroundInvoke
        Object aroundInvoke(InvocationContext ctx) throws Exception {
            return "a/" + ctx.proceed() + "/a";
        }

    }

    @Priority(2)
    @Interceptor
    @InterceptMe("bravo")
    static class InterceptMeBravo {

        @AroundInvoke
        Object aroundInvoke(InvocationContext ctx) throws Exception {
            return "b/" + ctx.proceed() + "/b";
        }

    }

    @Repeatable(InterceptMe.List.class)
    @InterceptorBinding
    @Target({ TYPE, METHOD, CONSTRUCTOR })
    @Retention(RUNTIME)
    @interface InterceptMe {

        String value();

        @Target({ TYPE, METHOD, CONSTRUCTOR })
        @Retention(RUNTIME)
        @interface List {
            InterceptMe[] value();
        }

    }

}
