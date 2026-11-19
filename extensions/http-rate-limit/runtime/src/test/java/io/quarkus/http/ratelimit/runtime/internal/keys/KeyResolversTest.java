package io.quarkus.http.ratelimit.runtime.internal.keys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.http.ratelimit.RateLimitContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.RoutingContext;

class KeyResolversTest {

    private static RateLimitContext context(HttpServerRequest request, SecurityIdentity identity) {
        RoutingContext rc = mock(RoutingContext.class);
        when(rc.request()).thenReturn(request);
        RateLimitContext ctx = mock(RateLimitContext.class);
        when(ctx.routingContext()).thenReturn(rc);
        when(ctx.identity()).thenReturn(Optional.ofNullable(identity));
        return ctx;
    }

    private static String resolve(io.quarkus.http.ratelimit.RateLimitKeyResolver r, RateLimitContext ctx) {
        return r.resolve(ctx).await().indefinitely().orElse(null);
    }

    @Test
    void ipResolverUsesHostAddressWithoutPort() {
        HttpServerRequest request = mock(HttpServerRequest.class);
        when(request.remoteAddress()).thenReturn(SocketAddress.inetSocketAddress(54321, "10.0.0.7"));
        assertThat(resolve(new IpKeyResolver(), context(request, null))).isEqualTo("ip:10.0.0.7");
    }

    @Test
    void ipResolverWithNullAddressIsBareNamespace() {
        HttpServerRequest request = mock(HttpServerRequest.class);
        when(request.remoteAddress()).thenReturn(null);
        assertThat(resolve(new IpKeyResolver(), context(request, null))).isEqualTo("ip:");
    }

    @Test
    void headerValueIsTrimmedAndHashed() {
        HttpServerRequest request = mock(HttpServerRequest.class);
        when(request.headers()).thenReturn(MultiMap.caseInsensitiveMultiMap().add("X-Api-Key", "  k1 "));
        // the value may be a credential: the key holds its SHA-256, never the value itself
        // printf k1 | sha256sum
        assertThat(resolve(new HeaderKeyResolver("X-Api-Key"), context(request, null)))
                .isEqualTo("header:sha256:6ab9f1eb8f7d3388f4f9d586f66e99fd54080df2c446f0e58668b09c08a16dd0");

        when(request.remoteAddress()).thenReturn(SocketAddress.inetSocketAddress(54321, "10.0.0.7"));
        when(request.headers()).thenReturn(MultiMap.caseInsensitiveMultiMap().add("X-Api-Key", "   "));
        assertThat(resolve(new HeaderKeyResolver("X-Api-Key"), context(request, null))).isEqualTo("ip:10.0.0.7");
    }

    /**
     * The limiter and the code that reads the header, an authentication mechanism or an override, could pick different
     * occurrences: a request that repeats the header shares one bucket with every other such request, whatever the
     * values.
     */
    @Test
    void repeatedHeaderSharesOneBucket() {
        HttpServerRequest request = mock(HttpServerRequest.class);
        when(request.headers()).thenReturn(MultiMap.caseInsensitiveMultiMap().add("X-Api-Key", "random")
                .add("x-api-key", "real"));
        assertThat(resolve(new HeaderKeyResolver("X-Api-Key"), context(request, null))).isEqualTo("header:repeated");
    }

    @Test
    void headerResolverAbsentHeaderFallsBackToClientAddress() {
        // like the principal key for an anonymous request: omitting the header must not escape the limit
        HttpServerRequest request = mock(HttpServerRequest.class);
        when(request.headers()).thenReturn(MultiMap.caseInsensitiveMultiMap());
        when(request.remoteAddress()).thenReturn(SocketAddress.inetSocketAddress(54321, "10.0.0.7"));
        assertThat(resolve(new HeaderKeyResolver("X-Api-Key"), context(request, null))).isEqualTo("ip:10.0.0.7");
    }

    @Test
    void principalResolverAnonymousFallsBackToClientAddress() {
        HttpServerRequest request = mock(HttpServerRequest.class);
        when(request.remoteAddress()).thenReturn(SocketAddress.inetSocketAddress(54321, "10.0.0.7"));
        SecurityIdentity anonymous = QuarkusSecurityIdentity.builder().setAnonymous(true).build();
        assertThat(resolve(new PrincipalKeyResolver(), context(request, anonymous))).isEqualTo("ip:10.0.0.7");
        assertThat(resolve(new PrincipalKeyResolver(), context(request, null))).isEqualTo("ip:10.0.0.7");
    }

    /**
     * The built-in resolvers answer at once and never fail, so their key resolution runs without a timer.
     */
    @Test
    void builtInResolversAreImmediate() {
        assertThat(ImmediateKeyResolver.class).isAssignableFrom(IpKeyResolver.class,
                GlobalKeyResolver.class, PrincipalKeyResolver.class, HeaderKeyResolver.class);
    }

    @Test
    void principalResolverReturnsName() {
        SecurityIdentity alice = QuarkusSecurityIdentity.builder()
                .setPrincipal(() -> "alice").build();
        assertThat(resolve(new PrincipalKeyResolver(), context(null, alice))).isEqualTo("principal:alice");
    }
}
