package io.quarkus.arc.test.buildextension.stereotypes;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Named;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.StereotypeRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

public class AdditionalStereotypesTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(ToBeStereotype.class, SimpleBinding.class, SimpleInterceptor.class, SomeBean.class)
            .stereotypeRegistrars(new MyStereotypeRegistrar())
            .annotationsTransformers(new MyAnnotationTrasnformer())
            .build();

    @Test
    public void test() {
        InstanceHandle<SomeBean> bean = Arc.container().select(SomeBean.class).getHandle();
        assertEquals(ApplicationScoped.class, bean.getBean().getScope());
        assertEquals("someBean", bean.getBean().getName());
        assertTrue(bean.getBean().isAlternative());
        assertEquals(11, bean.getBean().getPriority());

        SomeBean instance = bean.get();
        assertNotNull(instance);
        assertEquals("intercepted: hello", instance.hello());
    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @interface ToBeStereotype {
    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @InterceptorBinding
    @interface SimpleBinding {
    }

    @Interceptor
    @Priority(1)
    @SimpleBinding
    static class SimpleInterceptor {
        @AroundInvoke
        public Object invoke(InvocationContext context) throws Exception {
            return "intercepted: " + context.proceed();
        }
    }

    @ToBeStereotype
    static class SomeBean {
        public String hello() {
            return "hello";
        }
    }

    static class MyStereotypeRegistrar implements StereotypeRegistrar {
        @Override
        public Set<DotName> getAdditionalStereotypes() {
            return Set.of(DotName.createSimple(ToBeStereotype.class.getName()));
        }
    }

    static class MyAnnotationTrasnformer implements AnnotationsTransformer {
        @Override
        public boolean appliesTo(AnnotationTarget.Kind kind) {
            return kind == AnnotationTarget.Kind.CLASS;
        }

        @Override
        public void transform(TransformationContext transformationContext) {
            if (transformationContext.getTarget().asClass().name()
                    .equals(DotName.createSimple(ToBeStereotype.class.getName()))) {
                transformationContext.transform()
                        .add(ApplicationScoped.class)
                        .add(SimpleBinding.class)
                        .add(Named.class)
                        .add(Alternative.class)
                        .add(Priority.class, AnnotationValue.createIntegerValue("value", 11))
                        .done();
            }
        }

    }
}
