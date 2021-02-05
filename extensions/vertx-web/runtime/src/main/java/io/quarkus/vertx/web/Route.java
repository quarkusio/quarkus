package io.quarkus.vertx.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.vertx.web.Route.Routes;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * This annotation can be used to configure a reactive route in a declarative way.
 * <p>
 * The target business method must be non-private and non-static.
 * The annotated method can accept arguments of the following types:
 * <ul>
 * <li>{@code io.vertx.ext.web.RoutingContext}</li>
 * <li>{@code io.vertx.mutiny.ext.web.RoutingContext}</li>
 * <li>{@code io.quarkus.vertx.web.RoutingExchange}</li>
 * <li>{@code io.vertx.core.http.HttpServerRequest}</li>
 * <li>{@code io.vertx.core.http.HttpServerResponse}</li>
 * <li>{@code io.vertx.mutiny.core.http.HttpServerRequest}</li>
 * <li>{@code io.vertx.mutiny.core.http.HttpServerResponse}</li>
 * </ul>
 * Furthermore, it is possible to inject the request parameters into a method parameter annotated with
 * {@link io.quarkus.vertx.web.Param}:
 * 
 * <pre>
 * <code>
 *  class Routes {
 *      {@literal @Route}
 *      String hello({@literal @Param Optional<String>} name) {
 *         return "Hello " + name.orElse("world");
 *     }
 *  }
 *  </code>
 * </pre>
 * 
 * The request headers can be injected into a method parameter annotated with {@link io.quarkus.vertx.web.Header}:
 * 
 * <pre>
 * <code>
 *  class Routes {
 *     {@literal @Route}
 *     String helloFromHeader({@literal @Header("My-Header")} String header) {
 *         return "Hello " + header;
 *     }
 *  }
 *  </code>
 * </pre>
 * 
 * The request body can be injected into a method parameter annotated with {@link io.quarkus.vertx.web.Body}:
 * 
 * <pre>
 * <code>
 *  class Routes {
 *     {@literal @Route(produces = "application/json")}
 *     Person updatePerson({@literal @Body} Person person) {
 *        person.setName("Bob");
 *        return person;
 *     }
 *  }
 *  </code>
 * </pre>
 * 
 * If the annotated method returns {@code void} then it has to accept at least one argument that makes it possible to end the
 * response, for example {@link RoutingContext}.
 * If the annotated method does not return {@code void} then the arguments are optional.
 * <p>
 * If both {@link #path()} and {@link #regex()} are set the regular expression is used for matching.
 * <p>
 * If neither {@link #path()} nor {@link #regex()} is specified and the handler type is not {@link HandlerType#FAILURE} then the
 * route will match a path derived from the name of the method. This is done by de-camel-casing the name and then joining the
 * segments
 * with hyphens.
 */
@Repeatable(Routes.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Route {

    /**
     * Represents an HTTP method.
     * This enumeration only provides the common HTTP method.
     * For custom methods, you need to register the {@code route} manually on the managed {@code Router}.
     */
    enum HttpMethod {
        GET,
        HEAD,
        POST,
        PUT,
        DELETE,
        OPTIONS
    }

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
     * @see io.vertx.ext.web.Route#order(int)
     */
    int order() default 0;

    /**
     * Used for content-based routing.
     * <p>
     * If no {@code Content-Type} header is set then try to use the most acceptable content type.
     *
     * If the request does not contain an 'Accept' header and no content type is explicitly set in the
     * handler then the content type will be set to the first content type in the array.
     * 
     * @see io.vertx.ext.web.Route#produces(String)
     * @see RoutingContext#getAcceptableContentType()
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
         * A non-blocking request handler.
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
         * A failure handler can declare a single method parameter whose type extends {@link Throwable}. The type of the
         * parameter is used to match the result of {@link RoutingContext#failure()}.
         * 
         * <pre>
         * <code>
         *  class Routes {
         *     {@literal @Route(type = HandlerType.FAILURE)}
         *     void unsupported(UnsupportedOperationException e, HttpServerResponse response) {
         *        response.setStatusCode(501).end(e.getMessage());
         *     }
         *  }
         *  </code>
         * </pre>
         * 
         * <p>
         * If a failure handler declares neither a path nor a regex then the route matches all requests.
         * 
         * @see io.vertx.ext.web.Route#failureHandler(Handler)
         */
        FAILURE;

        public static HandlerType from(String value) {
            for (HandlerType handlerType : values()) {
                if (handlerType.toString().equals(value)) {
                    return handlerType;
                }
            }
            return null;
        }

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Routes {

        Route[] value();

    }

}
