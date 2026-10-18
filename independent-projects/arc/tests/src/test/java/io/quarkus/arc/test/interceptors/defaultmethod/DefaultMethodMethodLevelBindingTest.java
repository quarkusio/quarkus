package io.quarkus.arc.test.interceptors.defaultmethod;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.NoClassInterceptors;
import io.quarkus.arc.test.ArcTestContainer;

public class DefaultMethodMethodLevelBindingTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(
            ClassBinding.class, MethodBinding.class,
            ClassBindingInterceptor.class, MethodBindingInterceptor.class,
            MyInterface.class,
            InterfaceWithNoClassInterceptors.class,
            BeanWithClassBinding.class,
            BeanWithoutClassBinding.class,
            BeanOverridingDefault.class,
            BeanWithNoClassInterceptorsInterface.class);

    @Test
    void methodLevelBindingOnDefaultMethodIsApplied() {
        BeanWithoutClassBinding bean = Arc.container().instance(BeanWithoutClassBinding.class).get();
        assertThat(bean.defaultMethod()).isEqualTo("method:default");
    }

    @Test
    void methodLevelAndClassLevelBindingsOnDefaultMethod() {
        BeanWithClassBinding bean = Arc.container().instance(BeanWithClassBinding.class).get();
        assertThat(bean.defaultMethod()).isEqualTo("class:method:default");
    }

    @Test
    void abstractMethodBindingNotInherited() {
        BeanWithoutClassBinding bean = Arc.container().instance(BeanWithoutClassBinding.class).get();
        assertThat(bean.abstractWithBinding()).isEqualTo("abstract-impl");
    }

    @Test
    void overrideDefaultMethodUsesOwnBindings() {
        BeanOverridingDefault bean = Arc.container().instance(BeanOverridingDefault.class).get();
        assertThat(bean.defaultMethod()).isEqualTo("class:overridden");
    }

    @Test
    void noClassInterceptorsWithMethodLevelBindingOnDefault() {
        BeanWithNoClassInterceptorsInterface bean = Arc.container().instance(BeanWithNoClassInterceptorsInterface.class).get();
        assertThat(bean.noClassButMethodLevel()).isEqualTo("method:noclass");
    }

    @Target({ TYPE, METHOD })
    @Retention(RUNTIME)
    @InterceptorBinding
    @interface ClassBinding {
    }

    @Target({ TYPE, METHOD })
    @Retention(RUNTIME)
    @InterceptorBinding
    @interface MethodBinding {
    }

    @ClassBinding
    @Interceptor
    @Priority(1)
    static class ClassBindingInterceptor {
        @AroundInvoke
        Object intercept(InvocationContext ctx) throws Exception {
            return "class:" + ctx.proceed();
        }
    }

    @MethodBinding
    @Interceptor
    @Priority(2)
    static class MethodBindingInterceptor {
        @AroundInvoke
        Object intercept(InvocationContext ctx) throws Exception {
            return "method:" + ctx.proceed();
        }
    }

    interface MyInterface {
        @MethodBinding
        default String defaultMethod() {
            return "default";
        }

        @MethodBinding
        String abstractWithBinding();
    }

    interface InterfaceWithNoClassInterceptors {
        @MethodBinding
        @NoClassInterceptors
        default String noClassButMethodLevel() {
            return "noclass";
        }
    }

    @ClassBinding
    @ApplicationScoped
    static class BeanWithClassBinding implements MyInterface {
        @Override
        public String abstractWithBinding() {
            return "abstract-impl";
        }
    }

    @ApplicationScoped
    static class BeanWithoutClassBinding implements MyInterface {
        @Override
        public String abstractWithBinding() {
            return "abstract-impl";
        }
    }

    @ClassBinding
    @ApplicationScoped
    static class BeanOverridingDefault implements MyInterface {
        @Override
        public String defaultMethod() {
            return "overridden";
        }

        @Override
        public String abstractWithBinding() {
            return "abstract-impl";
        }
    }

    @ClassBinding
    @ApplicationScoped
    static class BeanWithNoClassInterceptorsInterface implements InterfaceWithNoClassInterceptors {
    }
}
