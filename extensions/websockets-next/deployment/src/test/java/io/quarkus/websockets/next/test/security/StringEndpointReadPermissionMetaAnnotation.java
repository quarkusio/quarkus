package io.quarkus.websockets.next.test.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.security.PermissionsAllowed;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@PermissionsAllowed("endpoint:read")
public @interface StringEndpointReadPermissionMetaAnnotation {

}
