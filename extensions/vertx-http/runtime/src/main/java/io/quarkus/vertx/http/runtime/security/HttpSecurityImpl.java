package io.quarkus.vertx.http.runtime.security;

import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.security.StringPermission;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.VertxHttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.security.HttpSecurityConfiguration.HttpPermissionCarrier;
import io.quarkus.vertx.http.runtime.security.HttpSecurityConfiguration.Policy;
import io.quarkus.vertx.http.runtime.security.annotation.BasicAuthentication;
import io.quarkus.vertx.http.runtime.security.annotation.FormAuthentication;
import io.quarkus.vertx.http.runtime.security.annotation.MTLSAuthentication;
import io.quarkus.vertx.http.security.HttpSecurity;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.ClientAuth;
import io.vertx.ext.web.RoutingContext;

final class HttpSecurityImpl implements HttpSecurity {

    private static final Logger LOG = Logger.getLogger(HttpSecurityImpl.class.getName());

    private final List<HttpPermissionCarrier> httpPermissions;
    private RolesMapping rolesMapping;

    HttpSecurityImpl() {
        this.rolesMapping = null;
        this.httpPermissions = new ArrayList<>();
    }

    @Override
    public HttpPermission path(String... patterns) {
        if (patterns == null || patterns.length == 0) {
            throw new IllegalArgumentException("Paths must not be empty");
        }
        var httpPermission = new HttpPermissionImpl(patterns);
        httpPermissions.add(httpPermission);
        return httpPermission;
    }

    @Override
    public HttpPermission get(String... paths) {
        return path(paths).methods("GET");
    }

    @Override
    public HttpPermission put(String... paths) {
        return path(paths).methods("PUT");
    }

    @Override
    public HttpPermission post(String... paths) {
        return path(paths).methods("POST");
    }

    @Override
    public HttpPermission delete(String... paths) {
        return path(paths).methods("DELETE");
    }

    @Override
    public HttpSecurity rolesMapping(Map<String, List<String>> roleToRoles) {
        if (rolesMapping != null) {
            throw new IllegalStateException("Roles mapping is already configured");
        }
        if (roleToRoles == null || roleToRoles.isEmpty()) {
            throw new IllegalArgumentException("Roles must not be empty");
        }
        roleToRoles.forEach(new BiConsumer<String, List<String>>() {
            @Override
            public void accept(String sourceRole, List<String> targetRoles) {
                if (sourceRole.isEmpty()) {
                    throw new IllegalArgumentException("Source role must not be empty");
                }
                if (targetRoles == null || targetRoles.isEmpty()) {
                    throw new IllegalArgumentException("Target roles for role '%s' must not be empty".formatted(sourceRole));
                }
            }
        });

        this.rolesMapping = RolesMapping.of(roleToRoles);
        return this;
    }

    @Override
    public HttpSecurity rolesMapping(String sourceRole, List<String> targetRoles) {
        if (sourceRole == null) {
            throw new IllegalArgumentException("Source role must not be null");
        }
        if (targetRoles == null) {
            throw new IllegalArgumentException("Target roles for role '%s' must not be null".formatted(sourceRole));
        }
        return rolesMapping(Map.of(sourceRole, targetRoles));
    }

    @Override
    public HttpSecurity rolesMapping(String sourceRole, String targetRole) {
        if (targetRole == null) {
            throw new IllegalArgumentException("Target role for role '%s' must not be null".formatted(sourceRole));
        }
        return rolesMapping(sourceRole, List.of(targetRole));
    }

    void addHttpPermissions(List<HttpPermissionCarrier> httpPermissions) {
        this.httpPermissions.addAll(httpPermissions);
    }

    private final class AuthorizationPolicy implements Authorization {

        private Policy policy = null;

        @Override
        public HttpSecurity permit() {
            validatePolicyNotSetYet();
            this.policy = new Policy(PermitSecurityPolicy.NAME, null);
            return HttpSecurityImpl.this;
        }

        @Override
        public HttpSecurity deny() {
            validatePolicyNotSetYet();
            this.policy = new Policy(DenySecurityPolicy.NAME, null);
            return HttpSecurityImpl.this;
        }

        @Override
        public HttpSecurity roles(Map<String, List<String>> roleToRoles, String... roles) {
            validatePolicyNotSetYet();
            if (roles == null || roles.length == 0) {
                throw new IllegalArgumentException("Roles must not be empty");
            }
            if (roleToRoles == null) {
                throw new IllegalArgumentException("Role to roles mapping must not be null");
            }
            this.policy = new Policy(null, new RolesAllowedHttpSecurityPolicy(Arrays.asList(roles), null, roleToRoles));
            return HttpSecurityImpl.this;
        }

        @Override
        public HttpSecurity roles(String... roles) {
            return roles(Map.of(), roles);
        }

        @Override
        public HttpSecurity permissions(Permission... permissions) {
            validatePolicyNotSetYet();
            if (permissions == null || permissions.length == 0) {
                throw new IllegalArgumentException("Permissions must not be empty");
            }
            policy = new Policy(null, new PermissionsHttpSecurityPolicy(permissions));
            return HttpSecurityImpl.this;
        }

        @Override
        public HttpSecurity permissions(String... permissionNames) {
            Objects.requireNonNull(permissionNames);
            StringPermission[] stringPermissions = new StringPermission[permissionNames.length];
            for (int i = 0; i < permissionNames.length; i++) {
                stringPermissions[i] = new StringPermission(permissionNames[i]);
            }
            return permissions(stringPermissions);
        }

        @Override
        public HttpSecurity policy(HttpSecurityPolicy httpSecurityPolicy) {
            validatePolicyNotSetYet();
            if (httpSecurityPolicy == null) {
                throw new IllegalArgumentException("HttpSecurityPolicy must not be null");
            }
            this.policy = new Policy(null, httpSecurityPolicy);
            return HttpSecurityImpl.this;
        }

        @Override
        public HttpSecurity policy(Predicate<SecurityIdentity> predicate) {
            return policy((identity, request) -> !identity.isAnonymous() && predicate.test(identity));
        }

        @Override
        public HttpSecurity policy(BiPredicate<SecurityIdentity, RoutingContext> predicate) {
            return policy(new SimpleHttpSecurityPolicy(predicate));
        }

        private HttpSecurity authenticated() {
            validatePolicyNotSetYet();
            this.policy = new Policy(AuthenticatedHttpSecurityPolicy.NAME, null);
            return HttpSecurityImpl.this;
        }

        private void validatePolicyNotSetYet() {
            if (policy != null) {
                throw new IllegalArgumentException("Policy has already been set");
            }
        }
    }

    private final class HttpPermissionImpl implements HttpPermission, HttpPermissionCarrier {

        private final String[] paths;
        private boolean shared;
        private boolean applyToJaxRs;
        private String[] methods;
        private HttpSecurityConfiguration.AuthenticationMechanism authMechanism;
        private AuthorizationPolicy authorizationPolicy;

        private HttpPermissionImpl(String[] paths) {
            this.paths = Arrays.copyOf(paths, paths.length);
            this.authMechanism = null;
            this.authorizationPolicy = null;
            this.shared = false;
            this.methods = null;
            this.applyToJaxRs = false;
        }

        private void requireAuthenticationByDefault() {
            // if someone selects authentication mechanism and doesn't configure
            // authorization policy, it is reasonable to expect they require authentication
            // similarly to what we do with @BasicAuthentication etc.
            if (authorizationPolicy == null) {
                authenticated();
            }
        }

        private void validateAuthenticationNotSetYet() {
            if (authMechanism != null) {
                throw new IllegalArgumentException("Authentication has already been set");
            }
        }

        private void validateAuthorizationNotSetYet() {
            if (authMechanism == null && authorizationPolicy != null) {
                throw new IllegalArgumentException("Authorization has already been set");
            }
        }

        @Override
        public HttpPermission basic() {
            // TODO: we can enable it automatically during the runtime instead of this
            boolean isBasicAuthDisabled = !Arc.container().select(BasicAuthenticationMechanism.class).isResolvable();
            if (isBasicAuthDisabled) {
                LOG.debug("Basic authentication is not available, you can enable it by setting " +
                        "the 'quarkus.http.auth.basic' configuration property to 'true'. " +
                        "Please ignore this warning if you provided a custom basic authentication mechanism.");
            }
            return authenticatedWith(BasicAuthentication.AUTH_MECHANISM_SCHEME);
        }

        @Override
        public HttpPermission form() {
            // TODO: we can enable it automatically during the runtime instead of this
            boolean isFormAuthDisabled = !Arc.container().select(FormAuthenticationMechanism.class).isResolvable();
            if (isFormAuthDisabled) {
                LOG.debug("Form-based authentication is not available, you can enable it by setting " +
                        "the 'quarkus.http.auth.form.enabled' configuration property to 'true'. " +
                        "Please ignore this warning if you provided a custom form-based authentication mechanism.");
            }
            return authenticatedWith(FormAuthentication.AUTH_MECHANISM_SCHEME);
        }

        @Override
        public HttpPermission mTLS() {
            boolean mTlsDisabled = ClientAuth.NONE.equals(getHttpBuildTimeConfig().tlsClientAuth());
            if (mTlsDisabled) {
                throw new IllegalStateException(
                        "TLS client authentication is not available, please set the 'quarkus.http.ssl.client-auth'"
                                + " configuration property to 'required' or 'request'");
            }
            return authenticatedWith(MTLSAuthentication.AUTH_MECHANISM_SCHEME);
        }

        @Override
        public HttpPermission bearer() {
            return authenticatedWith("Bearer");
        }

        @Override
        public HttpPermission webAuthn() {
            return authenticatedWith("webauthn");
        }

        @Override
        public HttpPermission authorizationCodeFlow() {
            return authenticatedWith("code");
        }

        @Override
        public HttpSecurity authenticated() {
            return authorization().authenticated();
        }

        @Override
        public HttpPermission authenticatedWith(String mechanism) {
            validateAuthenticationNotSetYet();
            requireAuthenticationByDefault();
            if (mechanism == null || mechanism.isBlank()) {
                throw new IllegalArgumentException("Authentication mechanism must not be null or blank");
            }
            this.authMechanism = new HttpSecurityConfiguration.AuthenticationMechanism(mechanism, null);
            return this;
        }

        @Override
        public HttpPermission authenticatedWith(HttpAuthenticationMechanism mechanism) {
            validateAuthenticationNotSetYet();
            requireAuthenticationByDefault();
            if (mechanism == null) {
                throw new IllegalArgumentException("HttpAuthenticationMechanism must not be null");
            }
            this.authMechanism = new HttpSecurityConfiguration.AuthenticationMechanism(null, mechanism);
            return this;
        }

        @Override
        public HttpPermission shared() {
            this.shared = true;
            return this;
        }

        @Override
        public HttpPermission applyToJaxRs() {
            this.applyToJaxRs = true;
            return this;
        }

        @Override
        public HttpPermission methods(String... httpMethods) {
            if (httpMethods == null || httpMethods.length == 0) {
                throw new IllegalArgumentException("HTTP methods must not be null or empty");
            }
            this.methods = Arrays.copyOf(httpMethods, httpMethods.length);
            return this;
        }

        @Override
        public AuthorizationPolicy authorization() {
            validateAuthorizationNotSetYet();
            this.authorizationPolicy = new AuthorizationPolicy();
            return authorizationPolicy;
        }

        @Override
        public HttpSecurity permit() {
            return authorization().permit();
        }

        @Override
        public HttpSecurity roles(String... roles) {
            return authorization().roles(roles);
        }

        @Override
        public HttpSecurity policy(HttpSecurityPolicy httpSecurityPolicy) {
            return authorization().policy(httpSecurityPolicy);
        }

        @Override
        public Set<String> getPaths() {
            return Set.of(paths);
        }

        @Override
        public boolean isShared() {
            return shared;
        }

        @Override
        public boolean shouldApplyToJaxRs() {
            return applyToJaxRs;
        }

        @Override
        public Set<String> getMethods() {
            return methods == null ? Set.of() : Set.of(methods);
        }

        @Override
        public HttpSecurityConfiguration.AuthenticationMechanism getAuthMechanism() {
            return authMechanism;
        }

        @Override
        public Policy getPolicy() {
            if (authorizationPolicy == null || authorizationPolicy.policy == null) {
                throw new IllegalStateException("Authorization Policy has not been set for paths: " + getPaths());
            }
            return authorizationPolicy.policy;
        }
    }

    private static final class PermissionsHttpSecurityPolicy implements HttpSecurityPolicy {

        private final Permission[] permissions;

        private PermissionsHttpSecurityPolicy(Permission[] permissions) {
            this.permissions = Arrays.copyOf(permissions, permissions.length);
        }

        @Override
        public Uni<CheckResult> checkPermission(RoutingContext request, Uni<SecurityIdentity> identityUni,
                AuthorizationRequestContext requestContext) {
            return identityUni.onItemOrFailure()
                    .transformToUni(new BiFunction<SecurityIdentity, Throwable, Uni<? extends CheckResult>>() {
                        @Override
                        public Uni<? extends CheckResult> apply(SecurityIdentity securityIdentity, Throwable throwable) {
                            if (throwable != null || securityIdentity == null || securityIdentity.isAnonymous()) {
                                if (throwable != null) {
                                    LOG.debug("Authentication failed, denying access", throwable);
                                }
                                return CheckResult.deny();
                            }
                            return logicalAndPermissionCheck(securityIdentity, 0);
                        }
                    });
        }

        private Uni<CheckResult> logicalAndPermissionCheck(SecurityIdentity securityIdentity, int i) {
            if (permissions.length == i) {
                return CheckResult.permit();
            }
            return securityIdentity
                    .checkPermission(permissions[i])
                    .onItemOrFailure().transformToUni(new BiFunction<Boolean, Throwable, Uni<? extends CheckResult>>() {
                        @Override
                        public Uni<? extends CheckResult> apply(Boolean aBoolean, Throwable throwable) {
                            if (throwable == null && Boolean.TRUE.equals(aBoolean)) {
                                return logicalAndPermissionCheck(securityIdentity, i + 1);
                            }
                            if (throwable != null) {
                                LOG.debug("Failed to check permission, denying access", throwable);
                            }
                            return CheckResult.deny();
                        }
                    });
        }
    }

    private static final class SimpleHttpSecurityPolicy implements HttpSecurityPolicy {

        private final BiPredicate<SecurityIdentity, RoutingContext> predicate;

        private SimpleHttpSecurityPolicy(BiPredicate<SecurityIdentity, RoutingContext> predicate) {
            this.predicate = predicate;
        }

        @Override
        public Uni<CheckResult> checkPermission(RoutingContext request, Uni<SecurityIdentity> identityUni,
                AuthorizationRequestContext requestContext) {
            return identityUni.onItemOrFailure()
                    .transform(new BiFunction<SecurityIdentity, Throwable, CheckResult>() {
                        @Override
                        public CheckResult apply(SecurityIdentity securityIdentity, Throwable throwable) {
                            if (securityIdentity == null) {
                                // shouldn't be possible happen...
                                return CheckResult.DENY;
                            }
                            if (throwable != null) {
                                LOG.debug("Failed to retrieve SecurityIdentity, denying access", throwable);
                                return CheckResult.DENY;
                            }
                            boolean deny;
                            try {
                                deny = !predicate.test(securityIdentity, request);
                            } catch (Exception e) {
                                LOG.debug("Failed to check permission, denying access", e);
                                deny = true;
                            }
                            return deny ? CheckResult.DENY : CheckResult.PERMIT;
                        }
                    });
        }
    }

    List<HttpPermissionCarrier> getHttpPermissions() {
        return List.copyOf(httpPermissions);
    }

    RolesMapping getRolesMapping() {
        return rolesMapping;
    }

    private static VertxHttpBuildTimeConfig getHttpBuildTimeConfig() {
        return ConfigProvider.getConfig().unwrap(SmallRyeConfig.class)
                .getConfigMapping(VertxHttpBuildTimeConfig.class);
    }
}
