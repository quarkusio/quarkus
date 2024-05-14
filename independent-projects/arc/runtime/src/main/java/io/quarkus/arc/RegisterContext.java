package io.quarkus.arc;

import jakarta.enterprise.context.NormalScope;
import jakarta.enterprise.context.spi.AlterableContext;
import jakarta.inject.Scope;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Registers an {@link AlterableContext} at build time.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
public @interface RegisterContext {
    /**
     * The {@link Scope} or {@link NormalScope} that this class manaages the context of.
     */
    Class<? extends Annotation> value();
}
