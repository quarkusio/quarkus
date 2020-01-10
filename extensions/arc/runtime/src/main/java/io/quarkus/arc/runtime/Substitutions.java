package io.quarkus.arc.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.enterprise.util.AnnotationLiteral;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.jdk.JDK8OrEarlier;

@TargetClass(value = AnnotationLiteral.class, onlyWith = JDK8OrEarlier.class)
final class Target_javax_enterprise_util_AnnotationLiteral {
    @Delete
    Method[] members;

    @Alias
    native Class<? extends Annotation> annotationType();

    @Substitute
    Method[] getMembers() {
        Class<? extends Annotation> annotationType = annotationType();
        Method[] members = annotationType.getDeclaredMethods();
        if (members.length > 0 && !annotationType.isAssignableFrom(this.getClass())) {
            // same error as original
            throw new RuntimeException(getClass() + " does not implement the annotation type with members "
                    + annotationType.getName());
        }
        return members;
    }
}

@SuppressWarnings("unused")
final class Substitutions {
    private Substitutions() {
    }
}
