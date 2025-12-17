package io.quarkus.it.hibernate.processor.data.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.security.PermissionsAllowed;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@PermissionsAllowed("find")
public @interface CanFindByName {

}
