package io.quarkus.spring.boot.properties.runtime;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * This annotation is added as a qualifier to bean producer methods that produce @ConfigProperties beans
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface SpringBootConfigProperties {

    final class Literal extends AnnotationLiteral<SpringBootConfigProperties> implements SpringBootConfigProperties {

        // used im generated code
        @SuppressWarnings("unused")
        public static final Literal INSTANCE = new Literal();

        private static final long serialVersionUID = 1L;

    }
}
