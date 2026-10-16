package io.quarkus.rest.client.reactive;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a REST Client method parameter as not being the request body parameter.
 * <p>
 * By default, a REST Client method parameter that is not recognized as another supported parameter type
 * (path, query, header, etc.) is treated as the request body. This annotation can be used to avoid this behavior.
 * One specific use case is to facilitate obtaining method parameters in
 * {@link org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam} when using method invocation
 * or parameter references.
 * <p>
 * This annotation can also be used as a meta-annotation on custom parameter annotations: any parameter
 * annotated with an annotation type that is itself annotated with {@code @NotBody} is also excluded from
 * the request body. This is useful for extensions that introduce custom parameter types without requiring
 * each parameter to be annotated individually.
 */
@Target({ ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NotBody {
}
