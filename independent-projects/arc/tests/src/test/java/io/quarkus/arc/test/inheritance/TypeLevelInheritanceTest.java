package io.quarkus.arc.test.inheritance;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.Typed;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InterceptorBinding;
import javax.interceptor.InvocationContext;
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
            .beanClasses(BasicBean.class, SubBean.class, FirstInterceptor.class,
                    SecondInterceptor.class, MyBinding.class, MyStereotype.class, SecondaryBinding.class, MyQualifier.class)
            .build();

    @Test
    public void testTypeInheritance() {
        ArcContainer container = Arc.container();

        assertEquals(0, FirstInterceptor.timesInvoked);
        assertEquals(0, SecondInterceptor.timesInvoked);

        BasicBean basicBean = container.instance(BasicBean.class, new MyQualifier.Literal()).get();
        assertNotNull(basicBean);
        basicBean.ping();
        assertEquals(1, FirstInterceptor.timesInvoked);
        assertEquals(1, SecondInterceptor.timesInvoked);

        SubBean subBean = container.instance(SubBean.class, new MyQualifier.Literal()).get();
        assertNotNull(subBean);
        subBean.ping();
        assertEquals(2, FirstInterceptor.timesInvoked);
        assertEquals(2, SecondInterceptor.timesInvoked);
    }

    @ApplicationScoped
    @MyBinding
    @MyQualifier
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

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Inherited
    @InterceptorBinding
    @interface SecondaryBinding {
    }

    @Inherited
    @Qualifier
    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @interface MyQualifier {

        static class Literal extends AnnotationLiteral<MyQualifier> implements MyQualifier {

        }
    }
}
