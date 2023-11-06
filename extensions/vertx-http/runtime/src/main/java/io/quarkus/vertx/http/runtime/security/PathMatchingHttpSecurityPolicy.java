package io.quarkus.vertx.http.runtime.security;

import java.util.HashMap;
import java.util.Map;

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

    // this map is planned for removal very soon as runtime named policies will make it obsolete
    private static final Map<String, HttpSecurityPolicy> HTTP_SECURITY_BUILD_TIME_POLICIES = new HashMap<>();

    PathMatchingHttpSecurityPolicy(HttpConfiguration httpConfig, HttpBuildTimeConfig buildTimeConfig) {
        super(httpConfig.auth.permissions, httpConfig.auth.rolePolicy, buildTimeConfig.rootPath,
                HTTP_SECURITY_BUILD_TIME_POLICIES);
    }

    static synchronized void replaceNamedBuildTimePolicies(Map<String, HttpSecurityPolicy> newPolicies) {
        HTTP_SECURITY_BUILD_TIME_POLICIES.clear();
        HTTP_SECURITY_BUILD_TIME_POLICIES.putAll(newPolicies);
    }

}
