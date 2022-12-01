package io.quarkus.runtime.graal;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(Constructor.class)
final class ConstructorReplacement {

    @Alias
    public Class<?> getDeclaringClass() {
        return null;
    }

    @Alias
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return null;
    }

    @Alias
    public Annotation[] getDeclaredAnnotations() {
        return null;
    }

    @Substitute
    public AnnotatedType getAnnotatedReturnType() {
        return new AnnotatedType() {
            @Override
            public Type getType() {
                return getDeclaringClass();
            }

            @Override
            public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                return ConstructorReplacement.class.getAnnotation(annotationClass);
            }

            @Override
            public Annotation[] getAnnotations() {
                return ConstructorReplacement.class.getDeclaredAnnotations();
            }

            @Override
            public Annotation[] getDeclaredAnnotations() {
                return ConstructorReplacement.class.getDeclaredAnnotations();
            }
        };
    }

}
