package io.quarkus.vertx.http;

import io.vertx.core.http.HttpServerOptions;

/**
 * Quarkus fires a CDI event of this type asynchronously when the HTTP server starts listening
 * on the configured host and port.
 *
 * <pre>
 * &#064;ApplicationScoped
 * public class MyListener {
 *
 *     void httpStarted(&#064;ObservesAsync HttpServerStart start) {
 *         // ...notified when the HTTP server starts listening
 *     }
 * }
 * </pre>
 */
public record HttpServerStart(HttpServerOptions options) {

}
