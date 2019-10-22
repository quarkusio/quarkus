package io.quarkus.elytron.security.jdbc.deployment;

import java.util.Optional;

import org.wildfly.security.auth.server.SecurityRealm;

import io.quarkus.agroal.deployment.DataSourceInitializedBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.elytron.security.deployment.SecurityRealmBuildItem;
import io.quarkus.elytron.security.jdbc.JdbcRecorder;
import io.quarkus.elytron.security.jdbc.JdbcSecurityRealmConfig;
import io.quarkus.runtime.RuntimeValue;

class ElytronSecurityJdbcProcessor {

    JdbcSecurityRealmConfig jdbc;

    @BuildStep(providesCapabilities = "io.quarkus.elytron.security.jdbc")
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.SECURITY_JDBC);
    }

    /**
     * Check to see if a JdbcRealmConfig was specified and enabled and create a
     * {@linkplain org.wildfly.security.auth.realm.JdbcSecurityRealmConfig}
     * runtime value to process the user/roles properties files. This also registers the names of the user/roles properties
     * files
     * to include the build artifact.
     *
     * @param recorder - runtime security recorder
     * @param securityRealm - the producer factory for the SecurityRealmBuildItem
     * @param dataSourceInitialized - ensure that Agroal DataSource is initialized first
     * @throws Exception - on any failure
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void configureJdbcRealmAuthConfig(JdbcRecorder recorder,
            BuildProducer<SecurityRealmBuildItem> securityRealm,
            BeanContainerBuildItem beanContainerBuildItem, //we need this to make sure ArC is initialized
            Optional<DataSourceInitializedBuildItem> dataSourceInitialized) throws Exception {
        if (jdbc.enabled) {
            RuntimeValue<SecurityRealm> realm = recorder.createRealm(jdbc);
            securityRealm.produce(new SecurityRealmBuildItem(realm, jdbc.realmName, null));
        }
    }

}
