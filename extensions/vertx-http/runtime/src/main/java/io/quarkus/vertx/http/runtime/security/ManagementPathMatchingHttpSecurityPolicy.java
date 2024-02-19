package io.quarkus.vertx.http.runtime.security;

import static io.quarkus.vertx.http.runtime.PolicyMappingConfig.AppliesTo.ALL;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;

import io.quarkus.runtime.Startup;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceConfiguration;

/**
 * A security policy that allows for matching of other security policies based on paths.
 *
 * This is used for the default path/method based RBAC.
 */
@Startup // do not initialize path matcher during first HTTP request
@Singleton
public class ManagementPathMatchingHttpSecurityPolicy extends AbstractPathMatchingHttpSecurityPolicy {

    ManagementPathMatchingHttpSecurityPolicy(ManagementInterfaceBuildTimeConfig buildTimeConfig,
            ManagementInterfaceConfiguration runTimeConfig, Instance<HttpSecurityPolicy> installedPolicies) {
        super(runTimeConfig.auth.permissions, runTimeConfig.auth.rolePolicy, buildTimeConfig.rootPath, installedPolicies, ALL);
    }

}
