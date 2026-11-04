package io.quarkus.http.ratelimit.runtime.internal.store;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.http.ratelimit.RateLimitLocalStoreCustomizer;
import io.quarkus.http.ratelimit.RateLimitStore;
import io.quarkus.http.ratelimit.runtime.internal.config.DefaultRateLimitPolicy;
import io.quarkus.http.ratelimit.runtime.internal.config.RateLimitRuleConfig;
import io.smallrye.common.annotation.Identifier;

/**
 * Selects the store of each rule: the rule's {@code store}, else {@code quarkus.http.rate-limit.store}. A non-local
 * store is a {@link RateLimitStore} bean annotated {@code @Identifier("<name>")}.
 */
@Singleton
public class StoreRegistry {

    private static final String LOCAL = "local";

    private final LocalRateLimitStore local;
    private final Instance<RateLimitStore> stores;

    @Inject
    public StoreRegistry(@Identifier(LOCAL) LocalRateLimitStore local, @Any Instance<RateLimitStore> stores) {
        this.local = local;
        this.stores = stores;
    }

    /**
     * Builds the in-memory caches of the rule now, so that the {@link RateLimitLocalStoreCustomizer}s run at startup.
     *
     * @param policy the policy of the rule
     */
    public RateLimitStore forRule(DefaultRateLimitPolicy policy, RateLimitRuleConfig rule) {
        String name = rule.store();
        if (LOCAL.equals(name)) {
            local.prepare(policy);
            return local;
        }
        return stores.select(Identifier.Literal.of(name)).get();
    }
}
