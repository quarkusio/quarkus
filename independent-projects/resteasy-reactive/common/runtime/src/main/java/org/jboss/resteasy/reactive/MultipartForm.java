package org.jboss.resteasy.reactive;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.ws.rs.BeanParam;

/**
 * Annotation to be used on POJOs meant to map to the various parts
 * of {@code multipart/form-data} HTTP bodies.
 * Each part of the POJO that should be mapped to a part of the body should be annotated with {@link RestForm}.
 * In order to facilitate conversion to the field's body type, {@link PartType} should be used to determine the media
 * type of the corresponding body part.
 *
 * It's important to take caution when using such POJOs to read via {@link org.jboss.resteasy.reactive.multipart.FileUpload},
 * {@code java.io.File} or {@code java.nio.file.Path} uploaded files in a blocking manner, that the resource method should be
 * annotated with {@link io.smallrye.common.annotation.Blocking}.
 *
 * @deprecated this annotation is not required anymore: you can use {@link BeanParam} or just omit it entirely, as long as your
 *             container class holds any annotated fields with {@link RestForm}, {@link RestCookie}, {@link RestHeader},
 *             {@link RestPath},
 *             {@link RestMatrix}, {@link RestQuery} or their JAX-RS equivalents.
 */
@Deprecated(forRemoval = true)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER, ElementType.TYPE })
public @interface MultipartForm {
    String value() default "";
}
