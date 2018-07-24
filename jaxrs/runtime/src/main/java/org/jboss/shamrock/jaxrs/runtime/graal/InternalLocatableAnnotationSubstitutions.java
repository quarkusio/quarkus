package org.jboss.shamrock.jaxrs.runtime.graal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "com.sun.xml.internal.bind.v2.model.annotation.LocatableAnnotation", onlyWith = InternalLocatableAnnotationSubstitutions.Selector.class)
final class InternalLocatableAnnotationSubstitutions {

    @Substitute
    public static <A extends Annotation> A create(A annotation, Locatable parentSourcePos ) {
        return annotation;
    }

    @Substitute
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        throw new RuntimeException("Not implemented");
    }


    @TargetClass(className = "com.sun.xml.internal.bind.v2.model.annotation.Locatable", onlyWith = InternalLocatableAnnotationSubstitutions.Selector.class)
    static final class Locatable {

    }

    static final class Selector implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName("com.sun.xml.internal.bind.v2.model.annotation.LocatableAnnotation");
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
    }
}
