package io.quarkus.vertx.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.vertx.http.runtime.filters.Filter;
import io.vertx.ext.web.RoutingContext;

/**
 * This annotation is used to define a "filter", i.e. a route called on every HTTP request.
 * <p>
 * The target business method must return {@code void} and accept exactly one argument of type {@link RoutingContext}.
 * <p>
 * Filters with higher priority are called first. The default priority for all annotation-based filters is
 * {@link #DEFAULT_PRIORITY}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RouteFilter {

    static final int DEFAULT_PRIORITY = 10;

    /**
     * Filters with higher priority are called first.
     * 
     * @return the priority
     * @see Filter#getPriority()
     */
    int value() default DEFAULT_PRIORITY;

}
