package io.quarkus.infinispan.client;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * Marker annotation to select the Infinispan client. For example, if the Infinispan connection is configured like so in
 * {@code application.properties}:
 *
 * <pre>
 * quarkus.infinispan-client.site-lon.hosts=localhost:11222
 * </pre>
 *
 * Then to inject the proper {@code RemoteCacheManager}, you would need to use {@code InfinispanClientName} like
 * indicated below:
 *
 * <pre>
 *     &#64Inject
 *     &#64InfinispanClientName("site-lon")
 *     RemoteCacheManager remoteCacheManager;
 * </pre>
 */
@Target({ METHOD, FIELD, PARAMETER, TYPE })
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface InfinispanClientName {
    /**
     * The remote cache manager name. If no value is provided the default cache manager is assumed.
     */
    String value();

    class Literal extends AnnotationLiteral<InfinispanClientName> implements InfinispanClientName {

        public static Literal of(String value) {
            return new Literal(value);
        }

        private final String value;

        public Literal(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }
}
