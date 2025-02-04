package io.quarkus.vertx.http.runtime.security;

import static io.quarkus.vertx.http.runtime.PolicyMappingConfig.AppliesTo.ALL;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;

import io.quarkus.runtime.Startup;
import io.quarkus.vertx.http.runtime.management.ManagementBuildTimeConfig;
import io.quarkus.vertx.http.runtime.management.ManagementConfig;

/**
 * A security policy that allows for matching of other security policies based on paths.
 * <p>
 * This is used for the default path/method based RBAC.
 */
@Startup // do not initialize path matcher during first HTTP request
@Singleton
public class ManagementPathMatchingHttpSecurityPolicy extends AbstractPathMatchingHttpSecurityPolicy {
    ManagementPathMatchingHttpSecurityPolicy(
            ManagementBuildTimeConfig managementBuildTimeConfig,
            ManagementConfig managementConfig, Instance<HttpSecurityPolicy> installedPolicies) {
        super(managementConfig.auth().permissions(), managementConfig.auth().rolePolicy(), managementBuildTimeConfig.rootPath(),
                installedPolicies, ALL);
    }
}
