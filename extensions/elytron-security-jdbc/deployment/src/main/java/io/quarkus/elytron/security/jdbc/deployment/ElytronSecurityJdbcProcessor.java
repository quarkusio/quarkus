package io.quarkus.elytron.security.jdbc.deployment;

import java.util.function.BooleanSupplier;

import org.wildfly.security.auth.server.SecurityRealm;

import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSecurityProviderBuildItem;
import io.quarkus.elytron.security.deployment.ElytronPasswordMarkerBuildItem;
import io.quarkus.elytron.security.jdbc.JdbcRecorder;
import io.quarkus.elytron.security.jdbc.JdbcSecurityRealmBuildTimeConfig;
import io.quarkus.elytron.security.jdbc.JdbcSecurityRealmRuntimeConfig;

class ElytronSecurityJdbcProcessor {

    private static final String PASSWORD_PROVIDER = "org.wildfly.security.password.WildFlyElytronPasswordProvider";

    @BuildStep()
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.SECURITY_JDBC);
    }

    @BuildStep(onlyIf = IsJdbcSecurityEnabled.class)
    void addPasswordProviderToNativeImage(BuildProducer<NativeImageSecurityProviderBuildItem> additionalProviders) {
        additionalProviders.produce(new NativeImageSecurityProviderBuildItem(PASSWORD_PROVIDER));
    }

    /**
     * Register a {@linkplain org.wildfly.security.auth.realm.jdbc.JdbcSecurityRealm} service built from the configured
     * principal queries.
     */
    @BuildStep(onlyIf = IsJdbcSecurityEnabled.class)
    void configureJdbcRealmAuthConfig(ServiceRegistrar serviceRegistrar,
            JdbcSecurityRealmBuildTimeConfig jdbcSecurityRealmBuildTimeConfig) {
        serviceRegistrar.forService(SecurityRealm.class, jdbcSecurityRealmBuildTimeConfig.realmName())
                .atPhase(Phase.DATA)
                .after(BeanContainer.class)
                .afterBuildItem(JdbcDataSourceBuildItem.class)
                .afterBuildItem(SyntheticBeansRuntimeInitBuildItem.class) // ATM relevant Agroal beans are synthetic
                .require(JdbcSecurityRealmRuntimeConfig.class)
                .onStart((ctx, runtimeConfig) -> JdbcRecorder.createRealm(runtimeConfig));
    }

    @BuildStep(onlyIf = IsJdbcSecurityEnabled.class)
    ElytronPasswordMarkerBuildItem marker() {
        return new ElytronPasswordMarkerBuildItem();
    }

    static final class IsJdbcSecurityEnabled implements BooleanSupplier {

        private final boolean enabled;

        IsJdbcSecurityEnabled(JdbcSecurityRealmBuildTimeConfig config) {
            this.enabled = config.enabled();
        }

        @Override
        public boolean getAsBoolean() {
            return enabled;
        }
    }
}
