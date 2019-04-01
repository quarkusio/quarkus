package io.quarkus.vertx.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.vertx.web.Route.Routes;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * Annotation used to configure a {@link io.quarkus.vertx.web.Route} in a declarative way.
 * <p>
 * The target business method must return {@code void} and accept exacly one argument of type {@link RoutingContext}.
 */
@Repeatable(Routes.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Route {

    /**
     *
     * @see Router#route(String)
     * @return the path
     */
    String path() default "";

    /**
     *
     * @see Router#routeWithRegex(String)
     * @return the path regex
     */
    String regex() default "";

    /**
     *
     * @see io.quarkus.vertx.web.Route#method(HttpMethod)
     * @return the HTTP methods
     */
    HttpMethod[] methods() default {};

    /**
     *
     * @return the type of the handler
     */
    HandlerType type() default HandlerType.NORMAL;

    /**
     * If set to {@link Integer#MIN_VALUE} the order of the route is not modified.
     * 
     * @see io.quarkus.vertx.web.Route#order(int)
     */
    int order() default Integer.MIN_VALUE;

    /**
     *
     * @see io.quarkus.vertx.web.Route#produces(String)
     * @return the produced content types
     */
    String[] produces() default {};

    /**
     *
     * @see io.quarkus.vertx.web.Route#consumes(String)
     * @return the consumed content types
     */
    String[] consumes() default {};

    enum HandlerType {

        /**
         * A request handler.
         *
         * @see io.vertx.ext.web.Route#handler(Handler)
         */
        NORMAL,
        /**
         * A blocking request handler.
         *
         * @see io.vertx.ext.web.Route#blockingHandler(Handler)
         */
        BLOCKING,
        /**
         * A failure handler.
         *
         * @see io.vertx.ext.web.Route#failureHandler(Handler)
         */
        FAILURE

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Routes {

        Route[] value();

    }

}
