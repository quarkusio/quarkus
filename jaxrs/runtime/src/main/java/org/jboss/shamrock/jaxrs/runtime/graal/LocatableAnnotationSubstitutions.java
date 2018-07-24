package org.jboss.shamrock.jaxrs.runtime.graal;

import java.lang.annotation.Annotation;
import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "com.sun.xml.bind.v2.model.annotation.LocatableAnnotation", onlyWith = LocatableAnnotationSubstitutions.Selector.class)
final class LocatableAnnotationSubstitutions {

    @Substitute
    public static <A extends Annotation> A create(A annotation, Locatable parentSourcePos ) {
        return annotation;
    }

    @TargetClass(className = "com.sun.xml.bind.v2.model.annotation.Locatable", onlyWith = LocatableAnnotationSubstitutions.Selector.class)
    static final class Locatable {

    }

    static final class Selector implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName("com.sun.xml.bind.v2.model.annotation.LocatableAnnotation");
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
    }
}
