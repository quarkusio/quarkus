package org.jboss.resteasy.reactive.server;

import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.UriInfo;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When used on a method, then an implementation of {@link jakarta.ws.rs.container.ContainerRequestFilter} is generated
 * that calls the annotated method with the proper arguments
 * <p>
 * The idea behind using this is to make it much to write a {@code ServerRequestFilter} as all the necessary information
 * is passed as arguments to the method instead of forcing the author to use a mix of {@code @Context} and programmatic CDI
 * look-ups.
 * <p>
 * An example filter could look like this:
 *
 * <pre>
 * public class CustomContainerRequestFilter {
 *
 *     private final SomeBean someBean;
 *
 *     // SomeBean will be automatically injected by CDI as long as SomeBean is a bean itself
 *     public CustomContainerRequestFilter(SomeBean someBean) {
 *         this.someBean = someBean;
 *     }
 *
 *     &#64;ServerRequestFilter
 *     public void whatever(UriInfo uriInfo, HttpHeaders httpHeaders) {
 *         // do something
 *     }
 * }
 * </pre>
 *
 * Methods annotated with {@code ServerRequestFilter} can declare any of the following parameters (in any order)
 * <ul>
 * <li>{@link ContainerRequestContext}
 * <li>{@link UriInfo}
 * <li>{@link HttpHeaders}
 * <li>{@link Request}
 * <li>{@link ResourceInfo}
 * <li>{@link SimpleResourceInfo}
 * </ul>
 *
 * The return type of the method must be either be of type {@code void}, {@code Response}, {@code RestResponse},
 * {@code Optional<Response>}, {@code Optional<RestResponse>},
 * {@code Uni<Void>}, {@code Uni<Response>} or
 * {@code Uni<RestResponse>}.
 * <ul>
 * <li>{@code void} should be used when filtering does not need to perform any blocking operations and the filter cannot abort
 * processing.
 * <li>{@code Response} or {@code RestResponse} should be used when filtering does not need to perform any blocking operations
 * and the filter cannot
 * abort
 * processing - in this case the processing will be aborted if the response is not {@code null}.
 * <li>{@code Optional<Response>} or {@code Optional<RestResponse>} should be used when filtering does not need to perform any
 * blocking operations but the filter
 * might abort processing - in this case processing is aborted when the {@code Optional} contains a {@code Response} payload.
 * <li>{@code Uni<Void>} should be used when filtering needs to perform a non-blocking operation but the filter cannot abort
 * processing.
 * Note that {@code Uni<Void>} can easily be produced using: {@code Uni.createFrom().nullItem()}
 * <li>{@code Uni<Response>} or {@code Uni<RestResponse>} should be used when filtering needs to perform a non-blocking
 * operation
 * and the filter
 * might abort processing - in this case processing is aborted when the {@code Uni} contains a {@code Response} payload.
 * </ul>
 *
 * Another important thing to note is that if {@link ContainerRequestContext} is used as a request parameter, calling
 * {@code abortWith}
 * is not allowed. You should use the proper response type if aborting processing is necessary.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ServerRequestFilter {

    /**
     * The priority with which this request filter will be executed
     */
    int priority() default Priorities.USER;

    /**
     * Whether the filter is a pre-matching filter
     *
     * Note that this setting and {@link ServerRequestFilter#readBody()} cannot be both set to true.
     */
    boolean preMatching() default false;

    /**
     * Normally {@link ContainerRequestFilter} classes are run by RESTEasy Reactive on the same thread as the Resource
     * Method - this means than when a Resource Method is annotated with {@link Blocking}, the filters will also be run
     * on a worker thread.
     * This is meant to be set to {@code true} if a filter should be run on the event-loop even if the target Resource
     * method is going to be run on the worker thread.
     * For this to work, this filter must be run before any of the filters when non-blocking is not required.
     */
    boolean nonBlocking() default false;

    /**
     * If set to {@code true}, the filter will be run after the body has been fully read but before any deserialization takes
     * place.
     *
     * Note that this change only affects Resource Methods that do result in reading the message body. For all other
     * Resource Methods that the filter applies to, it will be executed in normal fashion.
     *
     * Also note that this setting and {@link ServerRequestFilter#preMatching()} cannot be both set to true.
     */
    boolean readBody() default false;
}
