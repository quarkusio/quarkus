package io.quarkus.vertx.http;

import io.vertx.core.http.HttpServerConfig;

/**
 * Quarkus fires a CDI event of this type asynchronously when the Unix Domain Socket server starts listening
 * on the configured socket path.
 *
 * <pre>
 * &#064;ApplicationScoped
 * public class MyListener {
 *
 *     void domainSocketStarted(&#064;ObservesAsync DomainSocketServerStart start) {
 *         // ...notified when the Unix Domain Socket server starts listening
 *     }
 * }
 * </pre>
 */
public record DomainSocketServerStart(HttpServerConfig config) {

}
