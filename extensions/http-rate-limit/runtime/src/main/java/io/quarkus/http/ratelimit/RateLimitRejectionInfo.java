package io.quarkus.http.ratelimit;

import java.util.Optional;

/**
 * CDI event payload and rejection-writer input.
 * <p>
 * It is a snapshot taken when the request was rejected: it holds no reference to the request, so it is safe to keep
 * after the request has ended, for example in an {@code @ObservesAsync} observer.
 * <p>
 * As a CDI event, synchronous observers run on the event loop and must not block; asynchronous observers run on a
 * worker thread. The event fires for every rejection, so under a flood observers should count and defer the real work.
 *
 */
public interface RateLimitRejectionInfo {

    /**
     * @return the rule that rejected the request
     */
    String rule();

    /**
     * @return the phase in which it was rejected
     */
    RateLimitPhase phase();

    /**
     * @return the rate limit key; a key longer than 256 characters, or one that starts with
     *         {@code sha256:} or contains a brace, arrives as {@code sha256:} followed by the hex SHA-256 of the resolved key
     */
    String key();

    /**
     * @return the suggested wait, in seconds
     */
    long retryAfterSeconds();

    /**
     * @return the HTTP method of the request
     */
    String method();

    /**
     * @return the canonical path of the request, the one the rules were matched against: decoded, without matrix
     *         parameters or dot segments
     */
    String path();

    /**
     * @return the client address the {@code ip} key is built from: the host of the remote address, which honours
     *         {@code quarkus.http.proxy.proxy-address-forwarding} and may then be a forwarded host name; {@code null}
     *         if the request has no remote address
     */
    String remoteAddress();

    /**
     * @return the name of the authenticated, non-anonymous principal if it was already known when the request was
     *         rejected, which can only be the case after authentication; obtaining it never triggers authentication
     */
    Optional<String> principal();
}
