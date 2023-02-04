package io.quarkus.arc.test.interceptor;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class IgnoredPrivateInterceptedMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SimpleBean.class, SimpleInterceptor.class, Simple.class));

    @Inject
    SimpleBean simpleBean;

    @Test
    public void testBeanInvocation() {
        assertEquals("foo", simpleBean.foo());
    }

    @Singleton
    static class SimpleBean {

        @Simple
        private String foo() {
            return "foo";
        }

    }

    @Simple
    @Priority(1)
    @Interceptor
    public static class SimpleInterceptor {

        @AroundInvoke
        public Object mySuperCoolAroundInvoke(InvocationContext ctx) throws Exception {
            return "private" + ctx.proceed();
        }
    }

    @Target({ TYPE, METHOD })
    @Retention(RUNTIME)
    @Documented
    @InterceptorBinding
    public @interface Simple {

    }
}
