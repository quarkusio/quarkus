package io.quarkus.runtime.graal;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(Parameter.class)
final class ParameterReplacement {

    @Alias
    public Type getParameterizedType() {
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
    public AnnotatedType getAnnotatedType() {
        return new AnnotatedType() {
            @Override
            public Type getType() {
                return getParameterizedType();
            }

            @Override
            public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                return ParameterReplacement.class.getAnnotation(annotationClass);
            }

            @Override
            public Annotation[] getAnnotations() {
                return ParameterReplacement.class.getDeclaredAnnotations();
            }

            @Override
            public Annotation[] getDeclaredAnnotations() {
                return ParameterReplacement.class.getDeclaredAnnotations();
            }
        };
    }

}
