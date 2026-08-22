package io.quarkus.oidc.runtime;

import java.util.Collection;
import java.util.function.Predicate;

import io.quarkus.arc.Arc;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantConfigResolver;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

public interface OptionalOidcRouteHandler extends Handler<RoutingContext> {

    void updateIfPathChanged(OidcTenantConfig oidcConfig, TenantConfigContext tenant, TenantConfigBean tenantConfigBean);

    void initialize();

    void close();

    interface OptionalOidcRouteHandlerBuilder {

        OptionalOidcRouteHandler build(Collection<OidcTenantConfig> staticTenantConfigs, Vertx vertx);

        static boolean isRouteRequired(Collection<OidcTenantConfig> staticTenantConfigs,
                Predicate<OidcTenantConfig> tenantConfigPredicate) {
            if (staticTenantConfigs.stream().filter(OidcTenantConfig::tenantEnabled).anyMatch(tenantConfigPredicate)) {
                return true;
            }
            return Arc.requireContainer().select(TenantConfigResolver.class).isResolvable();
        }

    }

}
