package io.quarkus.test.component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.arc.All;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.test.InjectMock;

class TestFieldInjector {
    private static final Logger LOG = Logger.getLogger(TestFieldInjector.class);

    private final Field field;
    private final List<InstanceHandle<?>> unsetHandles;

    public static List<TestFieldInjector> inject(Class<?> testClass, Object testInstance) throws Exception {
        List<TestFieldInjector> result = new ArrayList<>();
        for (Field field : findInjectFields(testClass)) {
            result.add(new TestFieldInjector(field, testInstance));
        }
        return result;
    }

    public static void unset(Object testInstance, List<?> list) {
        List<TestFieldInjector> fieldInjectors = (List<TestFieldInjector>) list;
        for (TestFieldInjector fieldInjector : fieldInjectors) {
            for (InstanceHandle<?> handle : fieldInjector.unsetHandles) {
                if (handle.getBean() != null && handle.getBean().getScope().equals(Dependent.class)) {
                    try {
                        handle.destroy();
                    } catch (Exception e) {
                        LOG.errorf(e, "Unable to destroy the injected %s", handle.getBean());
                    }
                }
            }

            try {
                fieldInjector.field.set(testInstance, null);
            } catch (Exception e) {
                LOG.errorf(e, "Unable to unset the injected field %s", fieldInjector.field.getName());
            }
        }
    }

    static List<Field> findInjectFields(Class<?> testClass) {
        List<Class<? extends Annotation>> injectAnnotations;
        Class<? extends Annotation> deprecatedInjectMock = loadDeprecatedInjectMock();
        if (deprecatedInjectMock != null) {
            injectAnnotations = List.of(Inject.class, InjectMock.class, deprecatedInjectMock);
        } else {
            injectAnnotations = List.of(Inject.class, InjectMock.class);
        }
        return findFields(testClass, injectAnnotations);
    }

    static List<Field> findFields(Class<?> testClass, List<Class<? extends Annotation>> annotations) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = testClass;
        while (current.getSuperclass() != null) {
            for (Field field : current.getDeclaredFields()) {
                for (Class<? extends Annotation> annotation : annotations) {
                    if (field.isAnnotationPresent(annotation)) {
                        fields.add(field);
                        break;
                    }
                }
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    private TestFieldInjector(Field field, Object testInstance) throws Exception {
        this.field = field;

        ArcContainer container = Arc.container();
        BeanManager beanManager = container.beanManager();
        Type requiredType = field.getGenericType();
        Annotation[] qualifiers = getQualifiers(field, beanManager);

        Object injectedInstance;

        if (qualifiers.length > 0 && Arrays.stream(qualifiers).anyMatch(All.Literal.INSTANCE::equals)) {
            // Special handling for @Injec @All List
            if (isListRequiredType(requiredType)) {
                List<InstanceHandle<Object>> handles = container.listAll(requiredType, qualifiers);
                if (isTypeArgumentInstanceHandle(requiredType)) {
                    injectedInstance = handles;
                } else {
                    injectedInstance = handles.stream().map(InstanceHandle::get).collect(Collectors.toUnmodifiableList());
                }
                unsetHandles = QuarkusComponentTestExtension.cast(handles);
            } else {
                throw new IllegalStateException("Invalid injection point type: " + field);
            }
        } else {
            InstanceHandle<?> handle = container.instance(requiredType, qualifiers);
            if (field.isAnnotationPresent(Inject.class)) {
                if (handle.getBean().getKind() == io.quarkus.arc.InjectableBean.Kind.SYNTHETIC) {
                    throw new IllegalStateException(String
                            .format("The injected field %s expects a real component; but obtained: %s", field,
                                    handle.getBean()));
                }
            } else {
                if (!handle.isAvailable()) {
                    throw new IllegalStateException(String
                            .format("The injected field %s expects a mocked bean; but obtained null", field));
                } else if (handle.getBean().getKind() != io.quarkus.arc.InjectableBean.Kind.SYNTHETIC) {
                    throw new IllegalStateException(String
                            .format("The injected field %s expects a mocked bean; but obtained: %s", field,
                                    handle.getBean()));
                }
            }
            injectedInstance = handle.get();
            unsetHandles = List.of(handle);
        }

        if (!field.canAccess(testInstance)) {
            field.setAccessible(true);
        }

        field.set(testInstance, injectedInstance);
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Annotation> loadDeprecatedInjectMock() {
        try {
            return (Class<? extends Annotation>) Class.forName("io.quarkus.test.junit.mockito.InjectMock");
        } catch (Throwable e) {
            return null;
        }
    }

    private static Annotation[] getQualifiers(Field field, BeanManager beanManager) {
        List<Annotation> ret = new ArrayList<>();
        Annotation[] annotations = field.getDeclaredAnnotations();
        for (Annotation fieldAnnotation : annotations) {
            if (beanManager.isQualifier(fieldAnnotation.annotationType())) {
                ret.add(fieldAnnotation);
            }
        }
        return ret.toArray(new Annotation[0]);
    }

    private static boolean isListRequiredType(Type type) {
        if (type instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) type;
            return List.class.equals(parameterizedType.getRawType());
        }
        return false;
    }

    private static boolean isTypeArgumentInstanceHandle(Type type) {
        // List<String> -> String
        Type typeArgument = ((ParameterizedType) type).getActualTypeArguments()[0];
        if (typeArgument instanceof ParameterizedType) {
            return ((ParameterizedType) typeArgument).getRawType().equals(InstanceHandle.class);
        }
        return false;
    }
}
