package io.quarkus.vertx.http.runtime;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class AuthSessionConfig {

    /**
     * The encryption key that is used to store persistent logins (e.g. for form auth). Logins are stored in a persistent
     * cookie that is encrypted with AES-256 using a key derived from a SHA-256 hash of the key that is provided here.
     *
     * If no key is provided then an in-memory one will be generated, this will change on every restart though so it
     * is not suitable for production environments. This must be more than 16 characters long for security reasons
     */
    @ConfigItem
    public Optional<String> encryptionKey;

    /**
     * The inactivity (idle) timeout
     *
     * When inactivity timeout is reached, cookie is not renewed and a new login is enforced.
     */
    @ConfigItem(defaultValue = "PT30M")
    public Duration timeout;

    /**
     * How old a token can get before it will be replaced with a new token with an updated timeout, also
     * referred to as "renewal-timeout".
     *
     * Note that smaller values will result in slightly more server load (as new encrypted cookies will be
     * generated more often), however larger values affect the inactivity timeout as the timeout is set
     * when a cookie is generated.
     *
     * For example if this is set to 10 minutes, and the inactivity timeout is 30m, if a users last request
     * is when the cookie is 9m old then the actual timeout will happen 21m after the last request, as the timeout
     * is only refreshed when a new cookie is generated.
     *
     * In other words no timeout is tracked on the server side; the timestamp is encoded and encrypted in the cookie itself
     * and it is decrypted and parsed with each request.
     */
    @ConfigItem(defaultValue = "PT1M")
    public Duration newTokenInterval;
}
