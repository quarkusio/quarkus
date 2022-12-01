package io.quarkus.resteasy.reactive.jackson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.smallrye.common.annotation.Experimental;

/**
 * Annotation that can be used on fields (or getters) of POJOs returned by RESTEasy Reactive Resource methods, to signal that
 * then return type when serialized to JSON,
 * will not include fields whose security requirements match the current user's security scope.
 *
 * Warning: This annotation does not work when placed on a JAX-RS method that returns {@link javax.ws.rs.core.Response}.
 * Users that wish to use the feature and have the ability to configure the response of the JAX-RS method are advised to
 * use {@link org.jboss.resteasy.reactive.RestResponse}.
 */
@Experimental(value = "Remains to be determined if this is the best possible API for users to configure security of serialized fields")
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
public @interface SecureField {

    String[] rolesAllowed();
}
