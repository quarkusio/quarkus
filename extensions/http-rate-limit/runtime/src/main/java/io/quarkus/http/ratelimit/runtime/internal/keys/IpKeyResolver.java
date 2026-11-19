package io.quarkus.http.ratelimit.runtime.internal.keys;

import java.util.Optional;

import jakarta.inject.Singleton;

import io.quarkus.http.ratelimit.RateLimitContext;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;

@Singleton
@Identifier("ip")
public class IpKeyResolver implements ImmediateKeyResolver {

    @Override
    public Uni<Optional<String>> resolve(RateLimitContext context) {
        return Uni.createFrom().item(Optional.of(key(context.routingContext().request())));
    }

    /**
     * @return the key of the request: {@code ip:} followed by its {@linkplain #clientAddress client address}
     */
    public static String key(HttpServerRequest request) {
        String address = clientAddress(request);
        return "ip:" + (address == null ? "" : address);
    }

    /**
     * The client address the {@code ip} key is built from, also reported in rejections. It is the host as given, which
     * is an IP literal for a direct connection and may be a host name forwarded by a proxy; the resolved IP is not
     * used, as it is {@code null} for a forwarded host name and would put all such clients in one bucket.
     *
     * @return the client host, or {@code null} if the request has no remote address
     */
    public static String clientAddress(HttpServerRequest request) {
        SocketAddress address = request.remoteAddress();
        return address == null ? null : address.host();
    }
}
