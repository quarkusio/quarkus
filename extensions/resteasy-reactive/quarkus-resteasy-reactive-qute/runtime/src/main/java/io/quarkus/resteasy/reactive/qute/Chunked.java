package io.quarkus.resteasy.reactive.qute;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.qute.TemplateInstance;

/**
 * If used on a JAX-RS Resource method that returns {@link TemplateInstance} (or a {@link io.smallrye.mutiny.Uni} of
 * {@link TemplateInstance})
 * the {@code createMulti} method of {@link TemplateInstance} will be used.
 * This results in the response being chunked and avoids copy Strings.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Chunked {

}
