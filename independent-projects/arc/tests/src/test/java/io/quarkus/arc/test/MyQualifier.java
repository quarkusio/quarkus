package io.quarkus.arc.test;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.inject.Qualifier;

@Qualifier
@Inherited
@Target({ TYPE, METHOD, FIELD, PARAMETER })
@Retention(RUNTIME)
public @interface MyQualifier {

}
