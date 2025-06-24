package io.quarkus.vertx.http.runtime.security;

import static io.quarkus.vertx.http.runtime.PolicyMappingConfig.AppliesTo.JAXRS;
import static io.quarkus.vertx.http.runtime.security.AbstractPathMatchingHttpSecurityPolicy.duplicateNamedPoliciesNotAllowedEx;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.inject.Instance;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.BlockingSecurityExecutor;
import io.quarkus.security.spi.runtime.MethodDescription;
import io.quarkus.vertx.http.runtime.VertxHttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy.AuthorizationRequestContext;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy.CheckResult;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy.DefaultAuthorizationRequestContext;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * Decorates {@link AbstractPathMatchingHttpSecurityPolicy} path matching capabilities
 * with support for policies selected with {@link io.quarkus.vertx.http.security.AuthorizationPolicy}.
 * Decorator may only run after HTTP requests have been matched with the endpoint class method.
 * Extensions can make this class bean if they need it.
 */
public class JaxRsPathMatchingHttpSecurityPolicy {

    private final AbstractPathMatchingHttpSecurityPolicy delegate;
    private final boolean foundNoAnnotatedMethods;
    private final AuthorizationRequestContext requestContext;
    private final Map<String, HttpSecurityPolicy> policyNameToPolicy;
    private final AuthorizationPolicyStorage storage;

    JaxRsPathMatchingHttpSecurityPolicy(AuthorizationPolicyStorage storage,
            Instance<HttpSecurityPolicy> installedPolicies, VertxHttpConfig httpConfig,
            VertxHttpBuildTimeConfig httpBuildTimeConfig, BlockingSecurityExecutor blockingSecurityExecutor) {
        this.storage = storage;
        this.delegate = new AbstractPathMatchingHttpSecurityPolicy(
                HttpSecurityConfiguration.get(httpConfig).httpPermissions(),
                httpConfig.auth().rolePolicy(), httpBuildTimeConfig.rootPath(), installedPolicies, JAXRS);
        this.foundNoAnnotatedMethods = storage.getMethodToPolicyName().isEmpty();
        this.requestContext = new DefaultAuthorizationRequestContext(blockingSecurityExecutor);
        if (storage.getMethodToPolicyName().isEmpty()) {
            this.policyNameToPolicy = Map.of();
        } else {
            var allPolicies = new HashMap<String, HttpSecurityPolicy>();
            for (HttpSecurityPolicy installedPolicy : installedPolicies) {
                if (installedPolicy.name() != null) {
                    var previousPolicy = allPolicies.put(installedPolicy.name(), installedPolicy);
                    if (previousPolicy != null) {
                        throw duplicateNamedPoliciesNotAllowedEx(previousPolicy, installedPolicy);
                    }
                }
            }
            var annotationPoliciesOnly = new HashMap<String, HttpSecurityPolicy>();
            for (Map.Entry<MethodDescription, String> e : storage.getMethodToPolicyName().entrySet()) {
                var policyName = e.getValue();
                if (annotationPoliciesOnly.containsKey(policyName)) {
                    continue;
                }
                if (allPolicies.containsKey(policyName)) {
                    annotationPoliciesOnly.put(policyName, allPolicies.get(policyName));
                    continue;
                }
                var classAndMethodName = e.getKey().getClassName() + "#" + e.getKey().getMethodName();
                throw new RuntimeException("""
                        Endpoint '%s' requires named HttpSecurityPolicy '%s' specified with '@AuthorizationPolicy',
                        but no such policies has bean found. Please provide required policy as CDI bean.
                        """.formatted(classAndMethodName, policyName));
            }
            policyNameToPolicy = Map.copyOf(annotationPoliciesOnly);
        }
    }

    /**
     * @param securedMethodDesc method description
     * @return true if method is secured with {@link io.quarkus.vertx.http.security.AuthorizationPolicy}
     */
    public boolean requiresAuthorizationPolicy(MethodDescription securedMethodDesc) {
        return storage.requiresAuthorizationPolicy(securedMethodDesc);
    }

    /**
     * @return true if there is no point running {@link #checkPermission(RoutingContext, Uni, MethodDescription)}
     */
    public boolean hasNoPermissions() {
        return delegate.hasNoPermissions() && foundNoAnnotatedMethods;
    }

    /**
     * Applies {@link HttpSecurityPolicy} matched by path-matching rules
     * or by {@link io.quarkus.vertx.http.security.AuthorizationPolicy}.
     */
    public Uni<CheckResult> checkPermission(RoutingContext routingContext, Uni<SecurityIdentity> identity,
            MethodDescription description) {
        var authorizationPolicy = findAuthorizationPolicy(description);
        if (authorizationPolicy == null) {
            return delegate.checkPermissions(routingContext, identity, requestContext);
        } else {
            return delegate.checkPermissions(routingContext, identity, requestContext, authorizationPolicy);
        }
    }

    private HttpSecurityPolicy findAuthorizationPolicy(MethodDescription description) {
        if (description != null) {
            var policyName = storage.getMethodToPolicyName().get(description);
            if (policyName != null) {
                return policyNameToPolicy.get(policyName);
            }
        }
        return null;
    }

}
