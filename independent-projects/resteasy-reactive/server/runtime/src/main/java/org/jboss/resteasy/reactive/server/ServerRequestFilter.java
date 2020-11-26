package org.jboss.resteasy.reactive.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;

/**
 * When used on a method, then an implementation of {@link javax.ws.rs.container.ContainerRequestFilter} is generated
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
 * <li>{@link SimplifiedResourceInfo}
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ServerRequestFilter {

    /**
     * The priority with which this request filter will be executed
     */
    int priority() default Priorities.USER;

    /**
     * Whether or not the filter is a pre-matching filter
     */
    boolean preMatching() default false;
}
