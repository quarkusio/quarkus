package io.quarkus.arc.test.interceptor;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class FailingPrivateInterceptedMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SimpleBean.class, SimpleInterceptor.class, Simple.class))
            .assertException(e -> assertThat(e).isInstanceOf(DeploymentException.class)
                    .hasMessageContainingAll("@Simple will have no effect on method " + SimpleBean.class.getName() + ".foo",
                            "Either remove the annotation from the method",
                            "or turn this exception into a simple warning by setting configuration property 'quarkus.arc.fail-on-intercepted-private-method' to 'false'"));

    @Test
    public void testDeploymentFailed() {
        // This method should not be invoked
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
