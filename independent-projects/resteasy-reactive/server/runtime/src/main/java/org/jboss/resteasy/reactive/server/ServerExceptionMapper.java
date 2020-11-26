package org.jboss.resteasy.reactive.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;

/**
 * Annotation for handling exceptions in specific resource classes.
 *
 * Methods in a JAX-RS class annotated with this annotation will be used first when determining how to handle a thrown
 * exception.
 * This means that these methods take precedence over the global {@link javax.ws.rs.ext.ExceptionMapper} classes.
 *
 * In addition to the exception being handled, an annotated method can also be declare can declare any of the following
 * parameters (in any order)
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
public @interface ServerExceptionMapper {

    Class<? extends Throwable>[] value();
}
