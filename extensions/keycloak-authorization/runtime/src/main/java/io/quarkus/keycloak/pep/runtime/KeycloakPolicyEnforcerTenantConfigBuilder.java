package io.quarkus.keycloak.pep.runtime;

import static org.keycloak.representations.adapters.config.PolicyEnforcerConfig.EnforcementMode.ENFORCING;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.random.RandomGenerator;

import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.EnforcementMode;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.ScopeEnforcementMode;

import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerTenantConfig.KeycloakConfigPolicyEnforcer;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerTenantConfig.KeycloakConfigPolicyEnforcer.ClaimInformationPointConfig;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerTenantConfig.KeycloakConfigPolicyEnforcer.MethodConfig;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerTenantConfig.KeycloakConfigPolicyEnforcer.PathCacheConfig;
import io.quarkus.keycloak.pep.runtime.KeycloakPolicyEnforcerTenantConfig.KeycloakConfigPolicyEnforcer.PathConfig;
import io.quarkus.runtime.util.StringUtil;

public final class KeycloakPolicyEnforcerTenantConfigBuilder {
    private record KeycloakPolicyEnforcerTenantConfigImpl(int connectionPoolSize,
            KeycloakConfigPolicyEnforcer policyEnforcer) implements KeycloakPolicyEnforcerTenantConfig {
    }

    private record KeycloakConfigPolicyEnforcerImpl(Map<String, PathConfig> paths, EnforcementMode enforcementMode,
            boolean lazyLoadPaths, boolean httpMethodAsScope, ClaimInformationPointConfig claimInformationPoint,
            PathCacheConfig pathCache) implements KeycloakConfigPolicyEnforcer {
    }

    private record PathCacheConfigImpl(long lifespan, int maxEntries) implements PathCacheConfig {
    }

    private record ClaimInformationPointConfigImpl(Map<String, Map<String, String>> simpleConfig,
            Map<String, Map<String, Map<String, String>>> complexConfig) implements ClaimInformationPointConfig {
    }

    private record MethodConfigImpl(String method, List<String> scopes,
            ScopeEnforcementMode scopesEnforcementMode) implements MethodConfig {
    }

    private record PathConfigImpl(Optional<String> name, Optional<String> path, Optional<List<String>> paths,
            Map<String, MethodConfig> methods, EnforcementMode enforcementMode,
            ClaimInformationPointConfig claimInformationPoint) implements PathConfig {
    }

    private final Map<String, PathConfigBuilderImpl> paths = new HashMap<>();
    private int connectionPoolSize;
    private EnforcementMode enforcementMode;
    private boolean lazyLoadPaths;
    private boolean httpMethodAsScope;
    private ClaimInformationPointConfig claimInformationPoint;
    private PathCacheConfig pathCache;

    KeycloakPolicyEnforcerTenantConfigBuilder(KeycloakPolicyEnforcerTenantConfig originalConfig) {
        connectionPoolSize = originalConfig.connectionPoolSize();
        var policyEnforcer = originalConfig.policyEnforcer();
        enforcementMode = policyEnforcer.enforcementMode();
        lazyLoadPaths = policyEnforcer.lazyLoadPaths();
        httpMethodAsScope = policyEnforcer.httpMethodAsScope();
        claimInformationPoint = policyEnforcer.claimInformationPoint();
        pathCache = policyEnforcer.pathCache();
        policyEnforcer.paths().forEach(new BiConsumer<String, PathConfig>() {
            @Override
            public void accept(String name, PathConfig pathConfig) {
                paths.put(name, new PathConfigBuilderImpl(KeycloakPolicyEnforcerTenantConfigBuilder.this, pathConfig));
            }
        });
    }

    /**
     * Creates immutable {@link KeycloakPolicyEnforcerTenantConfig}.
     * Original builder can be safely re-used. The builder itself is not a thread-safe.
     *
     * @return KeycloakPolicyEnforcerTenantConfig
     */
    public KeycloakPolicyEnforcerTenantConfig build() {
        var pathConfigs = new HashMap<String, PathConfig>();
        paths.forEach(new BiConsumer<String, PathConfigBuilderImpl>() {
            @Override
            public void accept(String name, PathConfigBuilderImpl pathConfigBuilder) {
                var pathConfig = new PathConfigImpl(Optional.ofNullable(pathConfigBuilder.name), Optional.empty(),
                        Optional.of(List.copyOf(pathConfigBuilder.paths)), Map.copyOf(pathConfigBuilder.methods),
                        pathConfigBuilder.enforcementMode, pathConfigBuilder.claimInformationPointConfig);
                pathConfigs.put(name, pathConfig);
            }
        });
        return new KeycloakPolicyEnforcerTenantConfigImpl(connectionPoolSize, new KeycloakConfigPolicyEnforcerImpl(
                Map.copyOf(pathConfigs), enforcementMode, lazyLoadPaths, httpMethodAsScope, claimInformationPoint, pathCache));
    }

    public KeycloakPolicyEnforcerTenantConfigBuilder setConnectionPoolSize(int connectionPoolSize) {
        this.connectionPoolSize = connectionPoolSize;
        return this;
    }

    public KeycloakPolicyEnforcerTenantConfigBuilder setEnforcementMode(EnforcementMode enforcementMode) {
        Objects.requireNonNull(enforcementMode);
        this.enforcementMode = enforcementMode;
        return this;
    }

    public KeycloakPolicyEnforcerTenantConfigBuilder setLazyLoadPaths(boolean lazyLoadPaths) {
        this.lazyLoadPaths = lazyLoadPaths;
        return this;
    }

    public KeycloakPolicyEnforcerTenantConfigBuilder setHttpMethodAsScope(boolean httpMethodAsScope) {
        this.httpMethodAsScope = httpMethodAsScope;
        return this;
    }

    public KeycloakPolicyEnforcerTenantConfigBuilder setPathCache(long lifespan) {
        pathCache = new PathCacheConfigImpl(lifespan, pathCache == null ? 0 : pathCache.maxEntries());
        return this;
    }

    public KeycloakPolicyEnforcerTenantConfigBuilder setPathCache(int maxEntries) {
        pathCache = new PathCacheConfigImpl(pathCache == null ? 0L : pathCache.lifespan(), maxEntries);
        return this;
    }

    public KeycloakPolicyEnforcerTenantConfigBuilder setPathCache(int maxEntries, long lifespan) {
        pathCache = new PathCacheConfigImpl(lifespan, maxEntries);
        return this;
    }

    public KeycloakPolicyEnforcerTenantConfigBuilder setClaimInformationPoint(Map<String, Map<String, String>> simpleConfig) {
        claimInformationPoint = new ClaimInformationPointConfigImpl(simpleConfig == null ? Map.of() : Map.copyOf(simpleConfig),
                claimInformationPoint == null ? Map.of() : claimInformationPoint.complexConfig());
        return this;
    }

    public KeycloakPolicyEnforcerTenantConfigBuilder setClaimInformationPoint(Map<String, Map<String, String>> simpleConfig,
            Map<String, Map<String, Map<String, String>>> complexConfig) {
        claimInformationPoint = new ClaimInformationPointConfigImpl(simpleConfig == null ? Map.of() : Map.copyOf(simpleConfig),
                complexConfig == null ? Map.of() : Map.copyOf(complexConfig));
        return this;
    }

    /**
     * Adds path with {@param name).
     *
     * @param name refers to the 'path1' from the 'quarkus.keycloak.policy-enforcer.paths."path1".*' config properties
     * @param paths refers to the 'quarkus.keycloak.policy-enforcer.paths."paths".paths' configuration property
     * @return PathConfigBuilder
     */
    public PathConfigBuilder setNamedPaths(String name, String... paths) {
        Objects.requireNonNull(name);
        final PathConfigBuilderImpl pathConfigBuilder = this.paths.computeIfAbsent(name,
                new Function<String, PathConfigBuilderImpl>() {
                    @Override
                    public PathConfigBuilderImpl apply(String ignored) {
                        return new PathConfigBuilderImpl(KeycloakPolicyEnforcerTenantConfigBuilder.this, null);
                    }
                });
        if (paths != null && paths.length > 0) {
            pathConfigBuilder.paths.addAll(Set.of(paths));
        }
        return pathConfigBuilder;
    }

    /**
     * Adds paths with generated name.
     *
     * @param paths refers to the 'quarkus.keycloak.policy-enforcer.paths."paths".paths' configuration property
     * @return PathConfigBuilder
     */
    public PathConfigBuilder setPaths(String... paths) {
        final String name;
        if (paths == null || paths.length == 0) {
            name = getRandomPathName();
        } else {
            name = StringUtil.hyphenate(String.join("-", paths));
        }
        return setNamedPaths(name, paths);
    }

    private String getRandomPathName() {
        String name;
        do {
            name = "path" + RandomGenerator.getDefault().nextInt();
        } while (paths.containsKey(name));
        return name;
    }

    public sealed interface PathConfigBuilder permits PathConfigBuilderImpl {

        KeycloakPolicyEnforcerTenantConfigBuilder setClaimInformationPoint(Map<String, Map<String, String>> simpleConfig);

        KeycloakPolicyEnforcerTenantConfigBuilder setClaimInformationPoint(Map<String, Map<String, String>> simpleConfig,
                Map<String, Map<String, Map<String, String>>> complexConfig);

        /**
         * @param name permission name, as set by the 'quarkus.keycloak.policy-enforcer.paths."paths".name' config property
         * @return PathConfigBuilder
         */
        KeycloakPolicyEnforcerTenantConfigBuilder setPermissionName(String name);

        KeycloakPolicyEnforcerTenantConfigBuilder setEnforcementMode(EnforcementMode enforcementMode);

        /**
         * Makes this path specific for a POST method only.
         *
         * @param scopes optional scopes
         * @return KeycloakPolicyEnforcerTenantConfigBuilder
         */
        KeycloakPolicyEnforcerTenantConfigBuilder setPost(String... scopes);

        /**
         * Makes this path specific for a POST method only.
         *
         * @param scopes optional scopes
         * @param scopeEnforcementMode refers to the
         *        'quarkus.keycloak.policy-enforcer.paths."paths".methods."methods".scopes-enforcement-mode' config property
         * @return KeycloakPolicyEnforcerTenantConfigBuilder
         */
        KeycloakPolicyEnforcerTenantConfigBuilder setPost(ScopeEnforcementMode scopeEnforcementMode, String... scopes);

        /**
         * Makes this path specific for a HEAD method only.
         *
         * @param scopes optional scopes
         * @return KeycloakPolicyEnforcerTenantConfigBuilder
         */
        KeycloakPolicyEnforcerTenantConfigBuilder setHead(String... scopes);

        /**
         * Makes this path specific for a HEAD method only.
         *
         * @param scopes optional scopes
         * @param scopeEnforcementMode refers to the
         *        'quarkus.keycloak.policy-enforcer.paths."paths".methods."methods".scopes-enforcement-mode' config property
         * @return KeycloakPolicyEnforcerTenantConfigBuilder
         */
        KeycloakPolicyEnforcerTenantConfigBuilder setHead(ScopeEnforcementMode scopeEnforcementMode, String... scopes);

        /**
         * Makes this path specific for a GET method only.
         *
         * @param scopes optional scopes
         * @return KeycloakPolicyEnforcerTenantConfigBuilder
         */
        KeycloakPolicyEnforcerTenantConfigBuilder setGet(String... scopes);

        /**
         * Makes this path specific for a GET method only.
         *
         * @param scopes optional scopes
         * @param scopeEnforcementMode refers to the
         *        'quarkus.keycloak.policy-enforcer.paths."paths".methods."methods".scopes-enforcement-mode' config property
         * @return KeycloakPolicyEnforcerTenantConfigBuilder
         */
        KeycloakPolicyEnforcerTenantConfigBuilder setGet(ScopeEnforcementMode scopeEnforcementMode, String... scopes);

        /**
         * Makes this path specific for a PUT method only.
         *
         * @param scopes optional scopes
         * @return KeycloakPolicyEnforcerTenantConfigBuilder
         */
        KeycloakPolicyEnforcerTenantConfigBuilder setPut(String... scopes);

        /**
         * Makes this path specific for a PUT method only.
         *
         * @param scopes optional scopes
         * @param scopeEnforcementMode refers to the
         *        'quarkus.keycloak.policy-enforcer.paths."paths".methods."methods".scopes-enforcement-mode' config property
         * @return KeycloakPolicyEnforcerTenantConfigBuilder
         */
        KeycloakPolicyEnforcerTenantConfigBuilder setPut(ScopeEnforcementMode scopeEnforcementMode, String... scopes);

        /**
         * Makes this path specific for a PATCH method only.
         *
         * @param scopes optional scopes
         * @return KeycloakPolicyEnforcerTenantConfigBuilder
         */
        KeycloakPolicyEnforcerTenantConfigBuilder setPatch(String... scopes);

        /**
         * Makes this path specific for a PATCH method only.
         *
         * @param scopes optional scopes
         * @param scopeEnforcementMode refers to the
         *        'quarkus.keycloak.policy-enforcer.paths."paths".methods."methods".scopes-enforcement-mode' config property
         * @return KeycloakPolicyEnforcerTenantConfigBuilder
         */
        KeycloakPolicyEnforcerTenantConfigBuilder setPatch(ScopeEnforcementMode scopeEnforcementMode, String... scopes);

        /**
         * Make this path specific for the HTTP {@code method} only.
         *
         * @param method refers to the 'quarkus.keycloak.policy-enforcer.paths."paths".methods."methods".method' config property
         * @param scopeEnforcementMode refers to the
         *        'quarkus.keycloak.policy-enforcer.paths."paths".methods."methods".scopes-enforcement-mode' config property
         * @param scopes refers to the 'quarkus.keycloak.policy-enforcer.paths."paths".methods."methods".scopes' config property
         * @return KeycloakPolicyEnforcerTenantConfigBuilder
         */
        KeycloakPolicyEnforcerTenantConfigBuilder setMethod(String method, ScopeEnforcementMode scopeEnforcementMode,
                String... scopes);

        /**
         * Deletes all {@link MethodConfig} from the {@link PathConfig}.
         * Useful when you are adjusting existing path.
         *
         * @return KeycloakPolicyEnforcerTenantConfig
         */
        KeycloakPolicyEnforcerTenantConfigBuilder clearMethods();
    }

    private static final class PathConfigBuilderImpl implements PathConfigBuilder {
        private final KeycloakPolicyEnforcerTenantConfigBuilder builder;
        private final Map<String, MethodConfig> methods = new HashMap<>();
        private final Set<String> paths = new HashSet<>();
        private ClaimInformationPointConfig claimInformationPointConfig = new ClaimInformationPointConfigImpl(Map.of(),
                Map.of());
        private EnforcementMode enforcementMode = ENFORCING;
        private String name = null;

        private PathConfigBuilderImpl(KeycloakPolicyEnforcerTenantConfigBuilder builder, PathConfig pathConfig) {
            this.builder = builder;
            if (pathConfig != null) {
                this.methods.putAll(pathConfig.methods());
                this.claimInformationPointConfig = pathConfig.claimInformationPoint();
                this.paths.addAll(pathConfig.paths().orElse(List.of()));
                if (pathConfig.path().isPresent()) {
                    this.paths.add(pathConfig.path().get());
                }
                this.enforcementMode = pathConfig.enforcementMode();
            }
        }

        @Override
        public KeycloakPolicyEnforcerTenantConfigBuilder setClaimInformationPoint(
                Map<String, Map<String, String>> simpleConfig) {
            claimInformationPointConfig = new ClaimInformationPointConfigImpl(
                    simpleConfig == null ? Map.of() : Map.copyOf(simpleConfig),
                    claimInformationPointConfig == null ? Map.of() : claimInformationPointConfig.complexConfig());
            return builder;
        }

        @Override
        public KeycloakPolicyEnforcerTenantConfigBuilder setClaimInformationPoint(Map<String, Map<String, String>> simpleConfig,
                Map<String, Map<String, Map<String, String>>> complexConfig) {
            claimInformationPointConfig = new ClaimInformationPointConfigImpl(
                    simpleConfig == null ? Map.of() : Map.copyOf(simpleConfig),
                    complexConfig == null ? Map.of() : Map.copyOf(complexConfig));
            return builder;
        }

        @Override
        public KeycloakPolicyEnforcerTenantConfigBuilder setPermissionName(String name) {
            this.name = name;
            return builder;
        }

        @Override
        public KeycloakPolicyEnforcerTenantConfigBuilder setEnforcementMode(EnforcementMode enforcementMode) {
            Objects.requireNonNull(enforcementMode);
            this.enforcementMode = enforcementMode;
            return builder;
        }

        @Override
        public KeycloakPolicyEnforcerTenantConfigBuilder setPost(String... scopes) {
            return setPost(null, scopes);
        }

        @Override
        public KeycloakPolicyEnforcerTenantConfigBuilder setPost(ScopeEnforcementMode scopeEnforcementMode, String... scopes) {
            return setMethod("POST", scopeEnforcementMode, scopes);
        }

        @Override
        public KeycloakPolicyEnforcerTenantConfigBuilder setHead(String... scopes) {
            return setHead(null, scopes);
        }

        @Override
        public KeycloakPolicyEnforcerTenantConfigBuilder setHead(ScopeEnforcementMode scopeEnforcementMode, String... scopes) {
            return setMethod("HEAD", scopeEnforcementMode, scopes);
        }

        @Override
        public KeycloakPolicyEnforcerTenantConfigBuilder setGet(String... scopes) {
            return setGet(null, scopes);
        }

        @Override
        public KeycloakPolicyEnforcerTenantConfigBuilder setGet(ScopeEnforcementMode scopeEnforcementMode, String... scopes) {
            return setMethod("GET", scopeEnforcementMode, scopes);
        }

        @Override
        public KeycloakPolicyEnforcerTenantConfigBuilder setPut(String... scopes) {
            return setPut(null, scopes);
        }

        @Override
        public KeycloakPolicyEnforcerTenantConfigBuilder setPut(ScopeEnforcementMode scopeEnforcementMode, String... scopes) {
            return setMethod("PUT", scopeEnforcementMode, scopes);
        }

        @Override
        public KeycloakPolicyEnforcerTenantConfigBuilder setPatch(String... scopes) {
            return setPatch(null, scopes);
        }

        @Override
        public KeycloakPolicyEnforcerTenantConfigBuilder setPatch(ScopeEnforcementMode scopeEnforcementMode, String... scopes) {
            return setMethod("PATCH", scopeEnforcementMode, scopes);
        }

        @Override
        public KeycloakPolicyEnforcerTenantConfigBuilder setMethod(String method, ScopeEnforcementMode scopeEnforcementMode,
                String... scopes) {
            Objects.requireNonNull(method);
            if (scopeEnforcementMode == null) {
                // default enforcement scope is ALL
                scopeEnforcementMode = ScopeEnforcementMode.ALL;
            }
            methods.put(method.toLowerCase(), new MethodConfigImpl(method, List.of(scopes), scopeEnforcementMode));
            return builder;
        }

        @Override
        public KeycloakPolicyEnforcerTenantConfigBuilder clearMethods() {
            methods.clear();
            return builder;
        }
    }
}
