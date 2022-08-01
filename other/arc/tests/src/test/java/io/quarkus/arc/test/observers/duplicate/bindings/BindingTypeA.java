package io.quarkus.arc.test.observers.duplicate.bindings;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Qualifier
public @interface BindingTypeA {

    String value() default "";

    class BindingTypeABinding extends AnnotationLiteral<BindingTypeA> implements BindingTypeA {

        private final String value;

        public BindingTypeABinding(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }
}
