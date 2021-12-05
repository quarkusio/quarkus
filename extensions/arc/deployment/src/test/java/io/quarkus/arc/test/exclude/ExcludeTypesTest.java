package io.quarkus.arc.test.exclude;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InterceptorBinding;
import javax.interceptor.InvocationContext;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.exclude.bar.Bar;
import io.quarkus.arc.test.exclude.baz.bazzz.Baz;
import io.quarkus.test.QuarkusUnitTest;

public class ExcludeTypesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ExcludeTypesTest.class, Pong.class, Alpha.class, Bravo.class, Charlie.class, Bar.class,
                            Baz.class, Magic.class, MagicInterceptor.class)
                    .addAsResource(new StringAsset(
                            "quarkus.arc.exclude-types=Alpha,io.quarkus.arc.test.exclude.Bravo,io.quarkus.arc.test.exclude.bar.*,,io.quarkus.arc.test.exclude.baz.**,MagicInterceptor"),
                            "application.properties"));

    @Inject
    Instance<Object> instance;

    @Test
    public void testExcludeTypes() {
        assertFalse(instance.select(Alpha.class).isResolvable());
        assertFalse(instance.select(Bravo.class).isResolvable());
        assertFalse(instance.select(Bar.class).isResolvable());
        assertEquals("charlie", instance.select(Pong.class).get().ping());
    }

    @ApplicationScoped
    static class Alpha implements Pong {

        public String ping() {
            return "alpga";
        }

    }

    @ApplicationScoped
    static class Charlie implements Pong {

        @Magic
        public String ping() {
            return "charlie";
        }

    }

    public interface Pong {

        String ping();

    }

    @Magic
    @Interceptor
    public static class MagicInterceptor {

        @AroundInvoke
        public Object toUpperCase(InvocationContext context) throws Exception {
            return context.proceed().toString().toUpperCase();
        }

    }

    @InterceptorBinding
    @Target({ TYPE, METHOD })
    @Retention(RUNTIME)
    @interface Magic {

    }

}
