package io.quarkus.vertx.http.runtime.security;

import static io.quarkus.vertx.http.runtime.PolicyMappingConfig.AppliesTo.ALL;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;

import io.quarkus.runtime.Startup;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.HttpConfiguration;

/**
 * A security policy that allows for matching of other security policies based on paths.
 *
 * This is used for the default path/method based RBAC.
 */
@Startup // do not initialize path matcher during first HTTP request
@Singleton
public class PathMatchingHttpSecurityPolicy extends AbstractPathMatchingHttpSecurityPolicy implements HttpSecurityPolicy {

    PathMatchingHttpSecurityPolicy(HttpConfiguration httpConfig, HttpBuildTimeConfig buildTimeConfig,
            Instance<HttpSecurityPolicy> installedPolicies) {
        super(httpConfig.auth.permissions, httpConfig.auth.rolePolicy, buildTimeConfig.rootPath, installedPolicies, ALL);
    }

}
