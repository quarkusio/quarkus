package io.quarkus.vertx.http.runtime.security;

import static io.quarkus.vertx.http.runtime.PolicyMappingConfig.AppliesTo.ALL;
import static io.quarkus.vertx.http.runtime.security.HttpSecurityConfiguration.adaptToHttpPermissionCarriers;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;

import io.quarkus.vertx.http.runtime.management.ManagementConfig;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;

/**
 * A security policy that allows for matching of other security policies based on paths.
 * <p>
 * This is used for the default path/method based RBAC.
 */
@Singleton
public class ManagementPathMatchingHttpSecurityPolicy extends AbstractPathMatchingHttpSecurityPolicy {
    ManagementPathMatchingHttpSecurityPolicy(
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig,
            ManagementConfig managementConfig, Instance<HttpSecurityPolicy> installedPolicies) {
        super(adaptToHttpPermissionCarriers(managementConfig.auth().permissions()),
                managementConfig.auth().rolePolicy(), managementBuildTimeConfig.rootPath(), installedPolicies, ALL);
    }
}
