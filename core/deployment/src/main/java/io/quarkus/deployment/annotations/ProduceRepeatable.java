package io.quarkus.deployment.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The repeatable holder for {@link Produce}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ProduceRepeatable {
    /**
     * The {@link Produce} instances.
     *
     * @return the instances
     */
    Produce[] value();
}
