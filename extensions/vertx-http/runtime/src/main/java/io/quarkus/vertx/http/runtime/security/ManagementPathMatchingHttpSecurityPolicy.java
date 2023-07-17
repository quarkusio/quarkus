package io.quarkus.vertx.http.runtime.security;

import jakarta.inject.Singleton;

/**
 * A security policy that allows for matching of other security policies based on paths.
 *
 * This is used for the default path/method based RBAC.
 */
@Singleton
public class ManagementPathMatchingHttpSecurityPolicy extends AbstractPathMatchingHttpSecurityPolicy {
}
