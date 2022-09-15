package io.quarkus.arc.test.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class InjectionPointMetadataTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Controller.class, Controlled.class);

    @SuppressWarnings({ "unchecked", "rawtypes", "serial" })
    @Test
    public void testInjectionPointMetadata() {
        ArcContainer arc = Arc.container();
        Controller controller = arc.instance(Controller.class).get();

        // Field
        InjectionPoint injectionPoint = controller.controlled.injectionPoint;
        assertNotNull(injectionPoint);
        assertEquals(Controlled.class, injectionPoint.getType());
        Set<Annotation> qualifiers = injectionPoint.getQualifiers();
        assertEquals(1, qualifiers.size());
        assertEquals(Default.class, qualifiers.iterator().next().annotationType());
        Bean<?> bean = injectionPoint.getBean();
        assertNotNull(bean);
        assertTrue(bean.getTypes().stream().anyMatch(t -> t.equals(Controller.class)));
        assertNotNull(injectionPoint.getAnnotated());
        assertTrue(injectionPoint.getAnnotated() instanceof AnnotatedField);
        AnnotatedField<Controller> annotatedField = (AnnotatedField<Controller>) injectionPoint.getAnnotated();
        assertEquals("controlled", annotatedField.getJavaMember().getName());
        assertEquals(Controlled.class, annotatedField.getBaseType());
        assertEquals(2, annotatedField.getAnnotations().size());
        assertTrue(annotatedField.isAnnotationPresent(Inject.class));
        assertTrue(annotatedField.isAnnotationPresent(FooAnnotation.class));
        assertFalse(annotatedField.isAnnotationPresent(Deprecated.class));
        assertTrue(annotatedField.getAnnotation(Singleton.class) == null);
        assertTrue(annotatedField.getAnnotations(Singleton.class).isEmpty());

        // Method
        InjectionPoint methodInjectionPoint = controller.controlledMethod.injectionPoint;
        assertNotNull(methodInjectionPoint);
        assertEquals(Controlled.class, methodInjectionPoint.getType());
        assertTrue(methodInjectionPoint.getAnnotated() instanceof AnnotatedParameter);
        assertEquals(bean, methodInjectionPoint.getBean());
        AnnotatedParameter<Controller> methodParam = (AnnotatedParameter<Controller>) methodInjectionPoint.getAnnotated();
        assertEquals(0, methodParam.getPosition());
        assertEquals(Controller.class, methodParam.getDeclaringCallable().getJavaMember().getDeclaringClass());
        assertEquals("setControlled", methodParam.getDeclaringCallable().getJavaMember().getName());

        // Constructor
        InjectionPoint ctorInjectionPoint = controller.controlledCtor.injectionPoint;
        assertNotNull(ctorInjectionPoint);
        assertEquals(Controlled.class, ctorInjectionPoint.getType());
        assertTrue(ctorInjectionPoint.getAnnotated() instanceof AnnotatedParameter);
        assertEquals(bean, ctorInjectionPoint.getBean());
        AnnotatedParameter<Controller> ctorParam = (AnnotatedParameter<Controller>) ctorInjectionPoint.getAnnotated();
        assertEquals(1, ctorParam.getPosition());
        assertTrue(ctorParam.isAnnotationPresent(Singleton.class));
        assertTrue(ctorParam.getAnnotation(Singleton.class) != null);
        assertTrue(!ctorParam.getAnnotations(Singleton.class).isEmpty());
        assertEquals(1, ctorParam.getAnnotations().size());
        assertTrue(ctorParam.getDeclaringCallable() instanceof AnnotatedConstructor);
        assertEquals(Controller.class, ctorParam.getDeclaringCallable().getJavaMember().getDeclaringClass());

        // Instance
        InjectionPoint instanceInjectionPoint = controller.instanceControlled.get().injectionPoint;
        assertNotNull(instanceInjectionPoint);
        assertEquals(Controlled.class, instanceInjectionPoint.getType());
        qualifiers = instanceInjectionPoint.getQualifiers();
        assertEquals(1, qualifiers.size());
        assertEquals(Default.class, qualifiers.iterator().next().annotationType());
        bean = instanceInjectionPoint.getBean();
        assertNotNull(bean);
        assertTrue(bean.getTypes().stream().anyMatch(t -> t.equals(Controller.class)));
        assertNotNull(instanceInjectionPoint.getAnnotated());
        assertTrue(instanceInjectionPoint.getAnnotated() instanceof AnnotatedField);
        annotatedField = (AnnotatedField) instanceInjectionPoint.getAnnotated();
        assertEquals("instanceControlled", annotatedField.getJavaMember().getName());
        assertEquals(new TypeLiteral<Instance<Controlled>>() {
        }.getType(), annotatedField.getBaseType());
        assertTrue(annotatedField.isAnnotationPresent(Inject.class));
        assertTrue(annotatedField.getAnnotation(Singleton.class) == null);
        assertTrue(annotatedField.getAnnotations(Singleton.class).isEmpty());
        assertEquals(1, annotatedField.getAnnotations().size());
    }

    @SuppressWarnings({ "unchecked", "serial" })
    @Test
    public void testObserverInjectionPointMetadata() {
        AtomicReference<InjectionPoint> ip = new AtomicReference<>();
        Arc.container().beanManager().getEvent().select(new TypeLiteral<AtomicReference<InjectionPoint>>() {
        }).fire(ip);
        InjectionPoint injectionPoint = ip.get();
        assertNotNull(injectionPoint);
        assertEquals(Controlled.class, injectionPoint.getType());
        Set<Annotation> qualifiers = injectionPoint.getQualifiers();
        assertEquals(1, qualifiers.size());
        assertEquals(Default.class, qualifiers.iterator().next().annotationType());
        Assertions.assertNull(injectionPoint.getBean());
        assertNotNull(injectionPoint.getAnnotated());
        assertTrue(injectionPoint.getAnnotated() instanceof AnnotatedParameter);
        AnnotatedParameter<Controller> annotatedParam = (AnnotatedParameter<Controller>) injectionPoint.getAnnotated();
        assertEquals(Controlled.class, annotatedParam.getBaseType());
        assertEquals(1, annotatedParam.getAnnotations().size());
        assertFalse(annotatedParam.isAnnotationPresent(Inject.class));
        assertTrue(annotatedParam.isAnnotationPresent(FooAnnotation.class));
        assertTrue(annotatedParam.getAnnotation(Singleton.class) == null);
        assertTrue(annotatedParam.getAnnotations(Singleton.class).isEmpty());
    }

    @Singleton
    static class Controller {

        @FooAnnotation
        @Deprecated // This annotations should be ignored
        @Inject
        Controlled controlled;

        Controlled controlledMethod;

        Controlled controlledCtor;

        @Inject
        Instance<Controlled> instanceControlled;

        @Inject
        public Controller(BeanManager beanManager, @Singleton Controlled controlled) {
            this.controlledCtor = controlled;
        }

        @Inject
        void setControlled(Controlled controlled, BeanManager beanManager) {
            this.controlledMethod = controlled;
        }

        void observe(@Observes AtomicReference<InjectionPoint> ip, @FooAnnotation Controlled controlled) {
            ip.set(controlled.injectionPoint);
        }

    }

    @Dependent
    static class Controlled {

        @Inject
        InjectionPoint injectionPoint;

    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface FooAnnotation {

    }

}
