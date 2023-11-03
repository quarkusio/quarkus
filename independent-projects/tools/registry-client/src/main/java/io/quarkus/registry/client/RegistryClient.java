package io.quarkus.registry.client;

/**
 * Implements the basic queries a registry client is supposed to handle.
 * Although there are only a few kinds of queries, a registry is not required to support
 * all of them. For example, a registry may be configured to only provide platform extensions or
 * the other way around - provide only non-platform extensions but not platforms.
 */
public interface RegistryClient extends RegistryNonPlatformExtensionsResolver, RegistryPlatformExtensionsResolver,
        RegistryPlatformsResolver, RegistryConfigResolver, RegistryCache {
}
