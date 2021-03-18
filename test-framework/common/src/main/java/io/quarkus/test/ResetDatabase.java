package io.quarkus.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.interceptor.InterceptorBinding;

/**
 * Indicates that the database should be dropped and recreated
 * after the method is run.
 *
 * This requires an extension to be installed that has the ability
 * to update the database schema, namely Hibernate ORM, Flyway
 * or Liqibase.
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface ResetDatabase {

}
