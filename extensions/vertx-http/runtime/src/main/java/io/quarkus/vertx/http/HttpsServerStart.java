package io.quarkus.vertx.http;

import io.vertx.core.http.HttpServerOptions;

/**
 * Quarkus fires a CDI event of this type asynchronously when the HTTPS server starts listening
 * on the configured host and port.
 *
 * <pre>
 * &#064;ApplicationScoped
 * public class MyListener {
 *
 *     void httpsStarted(&#064;ObservesAsync HttpsServerStart start) {
 *         // ...notified when the HTTPS server starts listening
 *     }
 * }
 * </pre>
 */
public record HttpsServerStart(HttpServerOptions options) {

}
