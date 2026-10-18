package io.quarkus.rest.client.reactive;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to make calls requiring HTTP Basic Auth.
 * <p>
 *
 * An example method could look like the following:
 *
 * <pre>
 * {@code
 * &#64;ClientBasicAuth(username = "${service.username}", password = "${service.password}")
 * public interface SomeClient {
 *
 * }
 * }
 * </pre>
 *
 * where {@code service.username} and {@code service.password} are configuration properties that must be set at runtime
 * to the username and password that allow access to the service being called.
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ClientBasicAuth {

    String username();

    String password();
}
