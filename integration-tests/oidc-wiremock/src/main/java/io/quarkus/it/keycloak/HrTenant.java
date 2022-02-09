package io.quarkus.it.keycloak;

import java.lang.annotation.*;

import javax.interceptor.InterceptorBinding;

@Inherited
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface HrTenant {
}
