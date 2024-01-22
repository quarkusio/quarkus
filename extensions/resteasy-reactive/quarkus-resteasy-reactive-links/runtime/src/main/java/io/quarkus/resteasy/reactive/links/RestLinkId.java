package io.quarkus.resteasy.reactive.links;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a field on a resource to be a provider for 'id' parameters in associated Web links.
 * <p>
 * The RestLinkId annotation can be used at field level.
 * <p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface RestLinkId {
}
