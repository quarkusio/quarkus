package io.quarkus.elytron.security.jdbc.deployment;

import java.util.List;

import org.wildfly.security.auth.server.SecurityRealm;

import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSecurityProviderBuildItem;
import io.quarkus.elytron.security.deployment.ElytronPasswordMarkerBuildItem;
import io.quarkus.elytron.security.deployment.SecurityRealmBuildItem;
import io.quarkus.elytron.security.jdbc.JdbcRecorder;
import io.quarkus.elytron.security.jdbc.JdbcSecurityRealmBuildTimeConfig;
import io.quarkus.elytron.security.jdbc.JdbcSecurityRealmRuntimeConfig;
import io.quarkus.runtime.RuntimeValue;

class ElytronSecurityJdbcProcessor {

    private static final String PASSWORD_PROVIDER = "org.wildfly.security.password.WildFlyElytronPasswordProvider";

    @BuildStep()
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.SECURITY_JDBC);
    }

    @BuildStep
    void addPasswordProviderToNativeImage(BuildProducer<NativeImageSecurityProviderBuildItem> additionalProviders) {
        additionalProviders.produce(new NativeImageSecurityProviderBuildItem(PASSWORD_PROVIDER));
    }

    /**
     * Check to see if a JdbcRealmConfig was specified and enabled and create a
     * {@linkplain org.wildfly.security.auth.realm.JdbcSecurityRealmBuildTimeConfig}
     * runtime value to process the user/roles properties files. This also registers the names of the user/roles properties
     * files to include the build artifact.
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void configureJdbcRealmAuthConfig(JdbcRecorder recorder,
            JdbcSecurityRealmBuildTimeConfig jdbcSecurityRealmBuildTimeConfig,
            JdbcSecurityRealmRuntimeConfig jdbcSecurityRealmRuntimeConfig,
            BuildProducer<SecurityRealmBuildItem> securityRealm,
            BeanContainerBuildItem beanContainerBuildItem, //we need this to make sure ArC is initialized
            List<JdbcDataSourceBuildItem> dataSourcesConfigured) throws Exception {
        if (!jdbcSecurityRealmBuildTimeConfig.enabled) {
            return;
        }

        RuntimeValue<SecurityRealm> realm = recorder.createRealm(jdbcSecurityRealmRuntimeConfig);
        securityRealm.produce(new SecurityRealmBuildItem(realm, jdbcSecurityRealmBuildTimeConfig.realmName, null));
    }

    @BuildStep
    ElytronPasswordMarkerBuildItem marker(JdbcSecurityRealmBuildTimeConfig jdbcSecurityRealmBuildTimeConfig) {
        if (!jdbcSecurityRealmBuildTimeConfig.enabled) {
            return null;
        }
        return new ElytronPasswordMarkerBuildItem();
    }

}
