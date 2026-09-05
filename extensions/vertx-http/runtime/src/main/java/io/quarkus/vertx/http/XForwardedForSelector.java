package io.quarkus.vertx.http;

import java.util.List;

import io.vertx.core.MultiMap;
import io.vertx.core.net.SocketAddress;

/**
 * Selects the client address from the {@code X-Forwarded-For} header.
 * <p>
 * Register a single CDI bean implementing this interface. The same bean is applied to the main HTTP interface and,
 * when the corresponding {@code quarkus.management.proxy.*} properties are enabled, to the management interface.
 */
public interface XForwardedForSelector {

    /**
     * Returns the chosen client address, or {@code null} or a blank value to reject the request with a
     * {@code 400 Bad Request}. Surrounding whitespace is trimmed from the returned address.
     */
    String select(Context context);

    interface Context {

        /**
         * Returns the {@code X-Forwarded-For} values, merged across header lines, trimmed, without empty entries and
         * ordered leftmost first.
         */
        List<String> xForwardedForValues();

        /**
         * Returns the address of the directly connecting client, which cannot be forged.
         */
        SocketAddress connectionPeer();

        /**
         * Returns the request headers.
         */
        MultiMap headers();
    }
}
