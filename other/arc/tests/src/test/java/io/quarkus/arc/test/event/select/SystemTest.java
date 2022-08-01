package io.quarkus.arc.test.event.select;

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
public @interface SystemTest {
    String value() default "";

    class SystemTestLiteral extends AnnotationLiteral<SystemTest> implements SystemTest {

        private final String value;

        public SystemTestLiteral(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }
}
