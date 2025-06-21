package io.quarkus.vertx.http.runtime.security;

import static io.quarkus.vertx.http.runtime.PolicyMappingConfig.AppliesTo.ALL;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;

import io.quarkus.vertx.http.runtime.VertxHttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;

/**
 * A security policy that allows for matching of other security policies based on paths.
 * <p>
 * This is used for the default path/method based RBAC.
 */
@Singleton
public class PathMatchingHttpSecurityPolicy extends AbstractPathMatchingHttpSecurityPolicy implements HttpSecurityPolicy {
    PathMatchingHttpSecurityPolicy(
            VertxHttpConfig httpConfig, VertxHttpBuildTimeConfig httpBuildTimeConfig,
            Instance<HttpSecurityPolicy> installedPolicies) {
        super(HttpSecurityConfiguration.get(httpConfig).httpPermissions(),
                httpConfig.auth().rolePolicy(), httpBuildTimeConfig.rootPath(),
                installedPolicies, ALL);
    }
}
