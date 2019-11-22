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

/**
 * Annotation used to configure reactive routes in a declarative way.
 * <p>
 * The target business method must return {@code void} and accept exactly one argument. must return {@code void} and accept
 * exactly one argument. The type of the argument can be {@link io.vertx.ext.web.RoutingContext},
 * {@link io.vertx.reactivex.ext.web.RoutingContext} or {@link io.quarkus.vertx.web.RoutingExchange}.
 * <p>
 * If both {@link #path()} and {@link #regex()} are set the regular expression is used for matching.
 * <p>
 * If neither {@link #path()} nor {@link #regex()} is set the route will match a path derived from the name of the
 * method. This is done by de-camel-casing the name and then joining the segments with hyphens.
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
     * @see io.vertx.ext.web.Route#methods()
     * @return the HTTP methods
     */
    HttpMethod[] methods() default {};

    /**
     *
     * @return the type of the handler
     */
    HandlerType type() default HandlerType.NORMAL;

    /**
     * If set to a positive number, it indicates the place of the route in the chain.
     * 
     * @see io.vertx.ext.web.Route#order()
     */
    int order() default 0;

    /**
     * Used for content-based routing.
     * 
     * @see io.vertx.ext.web.Route#produces(String)
     * @return the produced content types
     */
    String[] produces() default {};

    /**
     * Used for content-based routing.
     *
     * @see io.vertx.ext.web.Route#consumes(String)
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
