package io.quarkus.runtime.graal;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(value = Field.class, onlyWith = GraalVersion19_2.class)
final class FieldReplacement {

    @Alias
    public Type getGenericType() {
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
                return getGenericType();
            }

            @Override
            public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                return FieldReplacement.class.getAnnotation(annotationClass);
            }

            @Override
            public Annotation[] getAnnotations() {
                return FieldReplacement.class.getDeclaredAnnotations();
            }

            @Override
            public Annotation[] getDeclaredAnnotations() {
                return FieldReplacement.class.getDeclaredAnnotations();
            }
        };
    }

}

final class GraalVersion19_2 implements BooleanSupplier {
    public boolean getAsBoolean() {
        final String version = System.getProperty("org.graalvm.version");
        return version.startsWith("19.2.");
    }
}
