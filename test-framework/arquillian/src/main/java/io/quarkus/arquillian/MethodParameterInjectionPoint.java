package io.quarkus.arquillian;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.AnnotatedCallable;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Qualifier;

public class MethodParameterInjectionPoint<T> implements InjectionPoint {
    private Method method;
    private int position;

    public MethodParameterInjectionPoint(Method method, int position) {
        this.method = method;
        this.position = position;
    }

    @Override
    public Bean<?> getBean() {
        return null;
    }

    @Override
    public Member getMember() {
        return method;
    }

    @Override
    public Set<Annotation> getQualifiers() {
        Set<Annotation> qualifiers = new HashSet<>();
        for (Annotation potentialQualifier : method.getParameterAnnotations()[position]) {
            if (potentialQualifier.annotationType().getAnnotation(Qualifier.class) != null) {
                qualifiers.add(potentialQualifier);
            }
        }
        if (qualifiers.size() == 0) {
            qualifiers.add(Default.Literal.INSTANCE);
        }
        return qualifiers;
    }

    @Override
    public Type getType() {
        return findTypeOrGenericType();
    }

    @Override
    public boolean isDelegate() {
        return false;
    }

    @Override
    public boolean isTransient() {
        return false;
    }

    @Override
    public Annotated getAnnotated() {
        return new ArgumentAnnotated<T>();
    }

    private Type findTypeOrGenericType() {
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        if (genericParameterTypes.length > 0) {
            return genericParameterTypes[position];
        }
        return method.getParameterTypes()[position];
    }

    private class ArgumentAnnotated<X> implements AnnotatedParameter<X> {

        @Override
        public AnnotatedCallable<X> getDeclaringCallable() {
            return null;
        }

        @Override
        public int getPosition() {
            return position;
        }

        @Override
        public <Y extends Annotation> Y getAnnotation(Class<Y> annotationType) {
            for (Annotation annotation : method.getParameterAnnotations()[position]) {
                if (annotation.annotationType() == annotationType) {
                    return annotationType.cast(annotation);
                }
            }
            return null;
        }

        @Override
        public Set<Annotation> getAnnotations() {
            return new HashSet<>(Arrays.asList(method.getParameterAnnotations()[position]));
        }

        @Override
        public Type getBaseType() {
            return getType();
        }

        @Override
        public Set<Type> getTypeClosure() {
            Set<Type> types = new HashSet<>();
            types.add(findTypeOrGenericType());
            types.add(Object.class);
            return types;
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
            return getAnnotation(annotationType) != null;
        }
    }
}
