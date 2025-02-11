package io.quarkus.it.oidc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.security.PermissionsAllowed;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@PermissionsAllowed(value = "mtls-jwt")
public @interface MtlsJwt {
}
