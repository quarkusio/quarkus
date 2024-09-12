package io.quarkus.resteasy.test.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.security.PermissionsAllowed;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@PermissionsAllowed(value = "farewell", permission = CustomPermissionWithExtraArgs.class, params = { "goodbye", "toWhom", "day",
        "place" })
public @interface CreateOrUpdate {

}
