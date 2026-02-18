package io.quarkus.arc.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.BeanManager;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.impl.EventBean;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class MockRecorder {
    public static volatile Set<Mock> beansToMock = new HashSet<>();

    public void registerMock(String declaringClass, String field, boolean deepMocks) throws Exception {
        Class<?> klass = Thread.currentThread().getContextClassLoader().loadClass(declaringClass);
        Field declaredField = klass.getDeclaredField(field);
        InstanceHandle<?> beanHandle = getBeanHandle(declaredField, declaringClass, "@InjectMock");
        if (beanHandle.getBean().getScope().equals(ApplicationScoped.class)) {
            // TODO - Pass beans indirectly because we need the Mockito API, which is not a direct dependency of Arc
            // We can probably move the mockito integration to be a full extension
            beansToMock.add(new Mock(declaredField.getGenericType(), beanHandle, deepMocks));
        }
    }

    static InstanceHandle<?> getBeanHandle(Field field, String declaringClass, String annotation) {
        Type fieldType = field.getGenericType();
        ArcContainer container = Arc.container();
        BeanManager beanManager = container.beanManager();
        Annotation[] qualifiers = getQualifiers(field, beanManager);

        InstanceHandle<?> handle = container.instance(fieldType, qualifiers);
        if (!handle.isAvailable()) {
            throw new IllegalStateException(
                    "Invalid use of " + annotation + " - could not resolve the bean of type: "
                            + fieldType.getTypeName() + ". Offending field is " + field.getName() + " of test class "
                            + declaringClass);
        }
        InjectableBean<?> bean = handle.getBean();
        if (!(bean instanceof EventBean)
                && !beanManager.isNormalScope(bean.getScope())) {
            throw new IllegalStateException(
                    "Invalid use of " + annotation
                            + " - the injected bean does not declare a CDI normal scope but: "
                            + handle.getBean().getScope().getName()
                            + ". Offending field is " + field.getName() + " of test class "
                            + declaringClass);
        }
        return handle;
    }

    static Annotation[] getQualifiers(Field fieldToMock, BeanManager beanManager) {
        List<Annotation> qualifiers = new ArrayList<>();
        Annotation[] fieldAnnotations = fieldToMock.getDeclaredAnnotations();
        for (Annotation fieldAnnotation : fieldAnnotations) {
            if (beanManager.isQualifier(fieldAnnotation.annotationType())) {
                qualifiers.add(fieldAnnotation);
            }
        }
        if (qualifiers.isEmpty()) {
            // Add @Default as if @InjectMock was a normal @Inject
            qualifiers.add(Default.Literal.INSTANCE);
        }
        return qualifiers.toArray(new Annotation[0]);
    }

    public record Mock(Type type, InstanceHandle<?> beanHandle, boolean deepMocks) {
    }
}
