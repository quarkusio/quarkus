package io.quarkus.test.security.oidc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface OidcSecurity {

    Claim[] claims() default {};

    boolean introspectionRequired() default false;

    TokenIntrospection[] introspection() default {};

    UserInfo[] userinfo() default {};

    ConfigMetadata[] config() default {};
}
