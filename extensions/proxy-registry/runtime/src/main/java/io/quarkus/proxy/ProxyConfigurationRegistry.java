package io.quarkus.proxy;

import java.util.Optional;

public interface ProxyConfigurationRegistry {
    /**
     * Signals that no proxy should be used.
     * No proxy configuration may use this name.
     */
    String NONE = "none";

    /**
     * If the given {@code name} is not empty, returns the named proxy configuration
     * if it exists and throws an exception if it doesn't exist. If the given {@code name}
     * is the special value {@link #NONE none}, returns a special result where the host and
     * port are {@code none:0}, type is {@code HTTP} and other methods return an empty result.
     * <p>
     * If the given {@code name} is empty, returns the default proxy configuration
     * if it exists and returns an {@linkplain Optional#empty() empty} result if it doesn't exist.
     *
     * @param name the name
     * @return the configuration, may be empty
     * @throws IllegalStateException if the named configuration does not exist
     */
    Optional<ProxyConfiguration> get(Optional<String> name);
}
