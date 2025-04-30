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

    public KeycloakPolicyEnforcerTenantConfigBuilder connectionPoolSize(int connectionPoolSize) {
        this.connectionPoolSize = connectionPoolSize;
        return this;
    }

    public KeycloakPolicyEnforcerTenantConfigBuilder enforcementMode(EnforcementMode enforcementMode) {
        Objects.requireNonNull(enforcementMode);
        this.enforcementMode = enforcementMode;
        return this;
    }

    public KeycloakPolicyEnforcerTenantConfigBuilder lazyLoadPaths(boolean lazyLoadPaths) {
        this.lazyLoadPaths = lazyLoadPaths;
        return this;
    }

    public KeycloakPolicyEnforcerTenantConfigBuilder httpMethodAsScope(boolean httpMethodAsScope) {
        this.httpMethodAsScope = httpMethodAsScope;
        return this;
    }

    public KeycloakPolicyEnforcerTenantConfigBuilder pathCache(long lifespan) {
        pathCache = new PathCacheConfigImpl(lifespan, pathCache == null ? 0 : pathCache.maxEntries());
        return this;
    }

    public KeycloakPolicyEnforcerTenantConfigBuilder pathCache(int maxEntries) {
        pathCache = new PathCacheConfigImpl(pathCache == null ? 0L : pathCache.lifespan(), maxEntries);
        return this;
    }

    public KeycloakPolicyEnforcerTenantConfigBuilder pathCache(int maxEntries, long lifespan) {
        pathCache = new PathCacheConfigImpl(lifespan, maxEntries);
        return this;
    }

    public PathCacheConfigBuilder pathCache() {
        return new PathCacheConfigBuilder(this);
    }

    public KeycloakPolicyEnforcerTenantConfigBuilder claimInformationPoint(Map<String, Map<String, String>> simpleConfig) {
        claimInformationPoint = new ClaimInformationPointConfigImpl(simpleConfig == null ? Map.of() : Map.copyOf(simpleConfig),
                claimInformationPoint == null ? Map.of() : claimInformationPoint.complexConfig());
        return this;
    }

    public KeycloakPolicyEnforcerTenantConfigBuilder claimInformationPoint(Map<String, Map<String, String>> simpleConfig,
            Map<String, Map<String, Map<String, String>>> complexConfig) {
        claimInformationPoint = new ClaimInformationPointConfigImpl(simpleConfig == null ? Map.of() : Map.copyOf(simpleConfig),
                complexConfig == null ? Map.of() : Map.copyOf(complexConfig));
        return this;
    }

    public ClaimInformationPointConfigBuilder<KeycloakPolicyEnforcerTenantConfigBuilder> claimInformationPoint() {
        return new ClaimInformationPointConfigBuilder<>() {

            @Override
            public KeycloakPolicyEnforcerTenantConfigBuilder build() {
                if (simpleConfig != null || complexConfig != null) {
                    return KeycloakPolicyEnforcerTenantConfigBuilder.this.claimInformationPoint(simpleConfig, complexConfig);
                }
                return KeycloakPolicyEnforcerTenantConfigBuilder.this;
            }
        };
    }

    /**
     * Adds path with {@param name).
     *
     * @param name refers to the 'path1' from the 'quarkus.keycloak.policy-enforcer.paths."path1".*' config properties
     * @param paths refers to the 'quarkus.keycloak.policy-enforcer.paths."paths".paths' configuration property
     * @param enforcementMode enforcement mode
     * @return PathConfigBuilder
     */
    public PathConfigBuilder namedPaths(String name, EnforcementMode enforcementMode, String... paths) {
        var pathConfigBuilder = namedPaths(name, paths);
        pathConfigBuilder.enforcementMode(enforcementMode);
        return pathConfigBuilder;
    }

    /**
     * Adds path with {@param name).
     *
     * @param name refers to the 'path1' from the 'quarkus.keycloak.policy-enforcer.paths."path1".*' config properties
     * @param paths refers to the 'quarkus.keycloak.policy-enforcer.paths."paths".paths' configuration property
     * @return PathConfigBuilder
     */
    public PathConfigBuilder namedPaths(String name, String... paths) {
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
     * @param enforcementMode enforcement mode
     * @return PathConfigBuilder
     */
    public PathConfigBuilder paths(EnforcementMode enforcementMode, String... paths) {
        var pathConfigBuilder = paths(paths);
        pathConfigBuilder.enforcementMode(enforcementMode);
        return pathConfigBuilder;
    }

    /**
     * Adds paths with generated name.
     *
     * @param paths refers to the 'quarkus.keycloak.policy-enforcer.paths."paths".paths' configuration property
     * @return PathConfigBuilder
     */
    public PathConfigBuilder paths(String... paths) {
        final String name;
        if (paths == null || paths.length == 0) {
            name = getRandomPathName();
        } else {
            name = StringUtil.hyphenate(String.join("-", paths));
        }
        return namedPaths(name, paths);
    }

    private String getRandomPathName() {
        String name;
        do {
            name = "path" + RandomGenerator.getDefault().nextInt();
        } while (paths.containsKey(name));
        return name;
    }

    public sealed interface PathConfigBuilder permits PathConfigBuilderImpl {

        KeycloakPolicyEnforcerTenantConfigBuilder claimInformationPoint(Map<String, Map<String, String>> simpleConfig);

        KeycloakPolicyEnforcerTenantConfigBuilder claimInformationPoint(Map<String, Map<String, String>> simpleConfig,
                Map<String, Map<String, Map<String, String>>> complexConfig);

        ClaimInformationPointConfigBuilder<PathConfigBuilder> claimInformationPoint();

        KeycloakPolicyEnforcerTenantConfigBuilder enforcementMode(EnforcementMode enforcementMode);

        /**
         * Makes this path specific for a POST method only.
         *
         * @param scopes optional scopes
         * @return KeycloakPolicyEnforcerTenantConfigBuilder
         */
        KeycloakPolicyEnforcerTenantConfigBuilder post(String... scopes);

        /**
         * Makes this path specific for a POST method only.
         *
         * @param scopes optional scopes
         * @param scopeEnforcementMode refers to the
         *        'quarkus.keycloak.policy-enforcer.paths."paths".methods."methods".scopes-enforcement-mode' config property
         * @return KeycloakPolicyEnforcerTenantConfigBuilder
         */
        KeycloakPolicyEnforcerTenantConfigBuilder post(ScopeEnforcementMode scopeEnforcementMode, String... scopes);

        /**
         * Makes this path specific for a HEAD method only.
         *
         * @param scopes optional scopes
         * @return KeycloakPolicyEnforcerTenantConfigBuilder
         */
        KeycloakPolicyEnforcerTenantConfigBuilder head(String... scopes);

        /**
         * Makes this path specific for a HEAD method only.
         *
         * @param scopes optional scopes
         * @param scopeEnforcementMode refers to the
         *        'quarkus.keycloak.policy-enforcer.paths."paths".methods."methods".scopes-enforcement-mode' config property
         * @return KeycloakPolicyEnforcerTenantConfigBuilder
         */
        KeycloakPolicyEnforcerTenantConfigBuilder head(ScopeEnforcementMode scopeEnforcementMode, String... scopes);

        /**
         * Makes this path specific for a GET method only.
         *
         * @param scopes optional scopes
         * @return KeycloakPolicyEnforcerTenantConfigBuilder
         */
        KeycloakPolicyEnforcerTenantConfigBuilder get(String... scopes);

        /**
         * Makes this path specific for a GET method only.
         *
         * @param scopes optional scopes
         * @param scopeEnforcementMode refers to the
         *        'quarkus.keycloak.policy-enforcer.paths."paths".methods."methods".scopes-enforcement-mode' config property
         * @return KeycloakPolicyEnforcerTenantConfigBuilder
         */
        KeycloakPolicyEnforcerTenantConfigBuilder get(ScopeEnforcementMode scopeEnforcementMode, String... scopes);

        /**
         * Makes this path specific for a PUT method only.
         *
         * @param scopes optional scopes
         * @return KeycloakPolicyEnforcerTenantConfigBuilder
         */
        KeycloakPolicyEnforcerTenantConfigBuilder put(String... scopes);

        /**
         * Makes this path specific for a PUT method only.
         *
         * @param scopes optional scopes
         * @param scopeEnforcementMode refers to the
         *        'quarkus.keycloak.policy-enforcer.paths."paths".methods."methods".scopes-enforcement-mode' config property
         * @return KeycloakPolicyEnforcerTenantConfigBuilder
         */
        KeycloakPolicyEnforcerTenantConfigBuilder put(ScopeEnforcementMode scopeEnforcementMode, String... scopes);

        /**
         * Makes this path specific for a PATCH method only.
         *
         * @param scopes optional scopes
         * @return KeycloakPolicyEnforcerTenantConfigBuilder
         */
        KeycloakPolicyEnforcerTenantConfigBuilder patch(String... scopes);

        /**
         * Makes this path specific for a PATCH method only.
         *
         * @param scopes optional scopes
         * @param scopeEnforcementMode refers to the
         *        'quarkus.keycloak.policy-enforcer.paths."paths".methods."methods".scopes-enforcement-mode' config property
         * @return KeycloakPolicyEnforcerTenantConfigBuilder
         */
        KeycloakPolicyEnforcerTenantConfigBuilder patch(ScopeEnforcementMode scopeEnforcementMode, String... scopes);

        /**
         * Make this path specific for the HTTP {@code method} only.
         *
         * @param method refers to the 'quarkus.keycloak.policy-enforcer.paths."paths".methods."methods".method' config property
         * @param scopeEnforcementMode refers to the
         *        'quarkus.keycloak.policy-enforcer.paths."paths".methods."methods".scopes-enforcement-mode' config property
         * @param scopes refers to the 'quarkus.keycloak.policy-enforcer.paths."paths".methods."methods".scopes' config property
         * @return KeycloakPolicyEnforcerTenantConfigBuilder
         */
        KeycloakPolicyEnforcerTenantConfigBuilder method(String method, ScopeEnforcementMode scopeEnforcementMode,
                String... scopes);

        /**
         * Creates builder for a path method.
         * Corresponds to configuration properties 'quarkus.keycloak.policy-enforcer.paths."paths".methods.*'.
         *
         * @return method builder
         */
        MethodConfigBuilder method();

        /**
         * @param name permission name, as set by the 'quarkus.keycloak.policy-enforcer.paths."paths".name' config property
         * @return PathConfigBuilder
         */
        PathConfigBuilder permissionName(String name);

        /**
         * Returns parent {@link KeycloakPolicyEnforcerTenantConfigBuilder}.
         * Calling this method is purely optional.
         *
         * @return parent builder
         */
        KeycloakPolicyEnforcerTenantConfigBuilder parent();
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
        public KeycloakPolicyEnforcerTenantConfigBuilder claimInformationPoint(
                Map<String, Map<String, String>> simpleConfig) {
            claimInformationPointConfig = new ClaimInformationPointConfigImpl(
                    simpleConfig == null ? Map.of() : Map.copyOf(simpleConfig),
                    claimInformationPointConfig == null ? Map.of() : claimInformationPointConfig.complexConfig());
            return builder;
        }

        @Override
        public KeycloakPolicyEnforcerTenantConfigBuilder claimInformationPoint(Map<String, Map<String, String>> simpleConfig,
                Map<String, Map<String, Map<String, String>>> complexConfig) {
            claimInformationPointConfig = new ClaimInformationPointConfigImpl(
                    simpleConfig == null ? Map.of() : Map.copyOf(simpleConfig),
                    complexConfig == null ? Map.of() : Map.copyOf(complexConfig));
            return builder;
        }

        @Override
        public ClaimInformationPointConfigBuilder<PathConfigBuilder> claimInformationPoint() {
            return new ClaimInformationPointConfigBuilder<>() {

                @Override
                public PathConfigBuilder build() {
                    if (simpleConfig != null || complexConfig != null) {
                        PathConfigBuilderImpl.this.claimInformationPoint(simpleConfig, complexConfig);
                    }
                    return PathConfigBuilderImpl.this;
                }
            };
        }

        @Override
        public PathConfigBuilder permissionName(String name) {
            this.name = name;
            return this;
        }

        @Override
        public KeycloakPolicyEnforcerTenantConfigBuilder enforcementMode(EnforcementMode enforcementMode) {
            Objects.requireNonNull(enforcementMode);
            this.enforcementMode = enforcementMode;
            return builder;
        }

        @Override
        public KeycloakPolicyEnforcerTenantConfigBuilder post(String... scopes) {
            return post(null, scopes);
        }

        @Override
        public KeycloakPolicyEnforcerTenantConfigBuilder post(ScopeEnforcementMode scopeEnforcementMode, String... scopes) {
            return method("POST", scopeEnforcementMode, scopes);
        }

        @Override
        public KeycloakPolicyEnforcerTenantConfigBuilder head(String... scopes) {
            return head(null, scopes);
        }

        @Override
        public KeycloakPolicyEnforcerTenantConfigBuilder head(ScopeEnforcementMode scopeEnforcementMode, String... scopes) {
            return method("HEAD", scopeEnforcementMode, scopes);
        }

        @Override
        public KeycloakPolicyEnforcerTenantConfigBuilder get(String... scopes) {
            return get(null, scopes);
        }

        @Override
        public KeycloakPolicyEnforcerTenantConfigBuilder get(ScopeEnforcementMode scopeEnforcementMode, String... scopes) {
            return method("GET", scopeEnforcementMode, scopes);
        }

        @Override
        public KeycloakPolicyEnforcerTenantConfigBuilder put(String... scopes) {
            return put(null, scopes);
        }

        @Override
        public KeycloakPolicyEnforcerTenantConfigBuilder put(ScopeEnforcementMode scopeEnforcementMode, String... scopes) {
            return method("PUT", scopeEnforcementMode, scopes);
        }

        @Override
        public KeycloakPolicyEnforcerTenantConfigBuilder patch(String... scopes) {
            return patch(null, scopes);
        }

        @Override
        public KeycloakPolicyEnforcerTenantConfigBuilder patch(ScopeEnforcementMode scopeEnforcementMode, String... scopes) {
            return method("PATCH", scopeEnforcementMode, scopes);
        }

        @Override
        public KeycloakPolicyEnforcerTenantConfigBuilder method(String method, ScopeEnforcementMode scopeEnforcementMode,
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
        public MethodConfigBuilder method() {
            return new MethodConfigBuilder(this);
        }

        @Override
        public KeycloakPolicyEnforcerTenantConfigBuilder parent() {
            return builder;
        }
    }

    public static final class PathCacheConfigBuilder {
        private final KeycloakPolicyEnforcerTenantConfigBuilder parent;
        Integer maxEntries = null;
        Long lifespan = null;

        private PathCacheConfigBuilder(KeycloakPolicyEnforcerTenantConfigBuilder parent) {
            this.parent = parent;
        }

        public PathCacheConfigBuilder lifespan(long lifespan) {
            this.lifespan = lifespan;
            return this;
        }

        public PathCacheConfigBuilder maxEntries(int maxEntries) {
            this.maxEntries = maxEntries;
            return this;
        }

        public KeycloakPolicyEnforcerTenantConfigBuilder build() {
            if (maxEntries == null && lifespan == null) {
                return parent;
            }
            if (maxEntries == null) {
                return parent.pathCache(lifespan);
            }
            if (lifespan == null) {
                return parent.pathCache(maxEntries);
            }
            return parent.pathCache(maxEntries, lifespan);
        }
    }

    public static abstract class ClaimInformationPointConfigBuilder<T> {
        protected Map<String, Map<String, String>> simpleConfig = null;
        protected Map<String, Map<String, Map<String, String>>> complexConfig = null;

        private ClaimInformationPointConfigBuilder() {
        }

        public ClaimInformationPointConfigBuilder<T> simpleConfig(Map<String, Map<String, String>> simpleConfig) {
            this.simpleConfig = simpleConfig;
            return this;
        }

        public ClaimInformationPointConfigBuilder<T> complexConfig(
                Map<String, Map<String, Map<String, String>>> complexConfig) {
            this.complexConfig = complexConfig;
            return this;
        }

        public abstract T build();
    }

    public static final class MethodConfigBuilder {
        private final PathConfigBuilder builder;
        private String method;
        private String[] scopes;
        private ScopeEnforcementMode scopesEnforcementMode;

        private MethodConfigBuilder(PathConfigBuilder builder) {
            this.builder = builder;
        }

        public MethodConfigBuilder method(String method) {
            this.method = method;
            return this;
        }

        public MethodConfigBuilder scopes(String... scopes) {
            this.scopes = scopes;
            return this;
        }

        public MethodConfigBuilder scopesEnforcementMode(ScopeEnforcementMode scopesEnforcementMode) {
            this.scopesEnforcementMode = scopesEnforcementMode;
            return this;
        }

        public PathConfigBuilder build() {
            Objects.requireNonNull(method);
            builder.method(method, scopesEnforcementMode, scopes == null ? new String[] {} : scopes);
            return builder;
        }
    }
}
