package io.quarkus.arc.test.inheritance;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.inject.Typed;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests type inheritence, e.g. that scopes, stereotypes, bindings and qualifiers are inherited so long as they
 * declare {@code @Inherited}
 *
 * Scope inheritance is tested separately, see ScopeInheritanceTest
 */
public class TypeLevelInheritanceTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder().removeUnusedBeans(false)
            .beanClasses(BasicBean.class, SubBean.class, FirstInterceptor.class, SecondInterceptor.class,
                    ThirdInterceptor.class, MyBinding.class, MyStereotype.class, MyStereotype2.class,
                    SecondaryBinding.class, MyQualifier.class, ThirdBinding.class, MyQualifier2.class)
            .build();

    @Test
    public void testTypeInheritance() {
        ArcContainer container = Arc.container();

        assertEquals(0, FirstInterceptor.timesInvoked);
        assertEquals(0, SecondInterceptor.timesInvoked);
        assertEquals(0, ThirdInterceptor.timesInvoked);

        BasicBean basicBean = container.instance(BasicBean.class, new MyQualifier.Literal(), new MyQualifier2.Literal()).get();
        assertNotNull(basicBean);
        basicBean.ping();
        assertEquals(1, FirstInterceptor.timesInvoked);
        assertEquals(1, SecondInterceptor.timesInvoked);
        assertEquals(1, ThirdInterceptor.timesInvoked);

        SubBean subBean = container.instance(SubBean.class, new MyQualifier.Literal(), new MyQualifier2.Literal()).get();
        assertNull(subBean);
        subBean = container.instance(SubBean.class, new MyQualifier.Literal()).get();
        assertNotNull(subBean);
        subBean.ping();
        assertEquals(2, FirstInterceptor.timesInvoked);
        assertEquals(2, SecondInterceptor.timesInvoked);
        assertEquals(1, ThirdInterceptor.timesInvoked);
    }

    @ApplicationScoped
    @MyBinding
    @MyQualifier
    @MyQualifier2
    @MyStereotype2
    @MyStereotype
    static class BasicBean {
        public void ping() {

        }
    }

    @Typed(SubBean.class)
    @ApplicationScoped
    static class SubBean extends BasicBean {

    }

    @MyBinding
    @Interceptor
    @Priority(1)
    static class FirstInterceptor {
        public static int timesInvoked = 0;

        @AroundInvoke
        Object aroundInvoke(InvocationContext ctx) throws Exception {
            timesInvoked++;
            return ctx.proceed();
        }
    }

    @ThirdBinding
    @Interceptor
    @Priority(2)
    static class ThirdInterceptor {
        public static int timesInvoked = 0;

        @AroundInvoke
        Object aroundInvoke(InvocationContext ctx) throws Exception {
            timesInvoked++;
            return ctx.proceed();
        }
    }

    @SecondaryBinding
    @Interceptor
    @Priority(1)
    static class SecondInterceptor {
        public static int timesInvoked = 0;

        @AroundInvoke
        Object aroundInvoke(InvocationContext ctx) throws Exception {
            timesInvoked++;
            return ctx.proceed();
        }
    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Inherited
    @InterceptorBinding
    @interface MyBinding {
    }

    @Stereotype
    @Inherited
    @SecondaryBinding
    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @interface MyStereotype {
    }

    @Stereotype
    @ThirdBinding
    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @interface MyStereotype2 {
    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @InterceptorBinding
    @interface SecondaryBinding {
    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @InterceptorBinding
    @interface ThirdBinding {
    }

    @Inherited
    @Qualifier
    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @interface MyQualifier {

        static class Literal extends AnnotationLiteral<MyQualifier> implements MyQualifier {

        }
    }

    @Qualifier
    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @interface MyQualifier2 {

        static class Literal extends AnnotationLiteral<MyQualifier2> implements MyQualifier2 {

        }
    }
}
