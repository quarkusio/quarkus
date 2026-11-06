package io.quarkus.micrometer.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD })
@Repeatable(MeterFilterConstraints.class)
public @interface MeterFilterConstraint {
    Class<?> applyTo();

    final class Literal extends AnnotationLiteral<MeterFilterConstraint> implements MeterFilterConstraint {
        private static final long serialVersionUID = 1L;
        private final Class<?> clazz;

        public Literal(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public Class<?> applyTo() {
            return clazz;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            } else if (other instanceof MeterFilterConstraint that) {
                return this.clazz.equals(that.applyTo());
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return 127 * "applyTo".hashCode() ^ this.clazz.hashCode();
        }
    }
}
