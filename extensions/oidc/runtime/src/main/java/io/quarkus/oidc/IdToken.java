package io.quarkus.oidc;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.inject.Qualifier;

@Qualifier
@Target({ FIELD, CONSTRUCTOR, METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface IdToken {
}
