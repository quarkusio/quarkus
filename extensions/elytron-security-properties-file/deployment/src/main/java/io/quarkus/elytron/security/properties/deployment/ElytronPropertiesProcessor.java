package io.quarkus.elytron.security.properties.deployment;

import org.jboss.logging.Logger;
import org.wildfly.security.auth.server.SecurityRealm;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.elytron.security.deployment.ElytronPasswordMarkerBuildItem;
import io.quarkus.elytron.security.deployment.SecurityRealmBuildItem;
import io.quarkus.elytron.security.properties.runtime.ElytronPropertiesFileRecorder;
import io.quarkus.elytron.security.properties.runtime.PropertiesRealmConfig;
import io.quarkus.elytron.security.properties.runtime.SecurityUsersConfig;
import io.quarkus.runtime.RuntimeValue;

/**
 * The build time process for the security aspects of the deployment. This creates {@linkplain BuildStep}s for integration
 * with the Elytron security services. This supports the Elytron
 * {@linkplain org.wildfly.security.auth.realm.LegacyPropertiesSecurityRealm}
 * and {@linkplain org.wildfly.security.auth.realm.SimpleMapBackedSecurityRealm} realm implementations. Others could be
 * added by creating an extension that produces a SecurityRealmBuildItem for the realm.
 *
 */
class ElytronPropertiesProcessor {
    private static final Logger log = Logger.getLogger(ElytronPropertiesProcessor.class.getName());

    SecurityUsersConfig propertiesConfig;

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.SECURITY_PROPERTIES_FILE);
    }

    /**
     * Check to see if a PropertiesRealmConfig was specified and enabled and create a
     * {@linkplain org.wildfly.security.auth.realm.LegacyPropertiesSecurityRealm}
     * runtime value to process the user/roles properties files. This also registers the names of the user/roles properties
     * files
     * to include the build artifact.
     *
     * @param recorder - runtime security recorder
     * @param securityRealm - the producer factory for the SecurityRealmBuildItem
     * @return the AuthConfigBuildItem for the realm authentication mechanism if there was an enabled PropertiesRealmConfig,
     *         null otherwise
     * @throws Exception - on any failure
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void configureFileRealmAuthConfig(ElytronPropertiesFileRecorder recorder,
            BuildProducer<SecurityRealmBuildItem> securityRealm) throws Exception {
        if (propertiesConfig.file().enabled()) {
            PropertiesRealmConfig realmConfig = propertiesConfig.file();
            log.debugf("Configuring from PropertiesRealmConfig, users=%s, roles=%s", realmConfig.users(),
                    realmConfig.roles());
            // Have the runtime recorder create the LegacyPropertiesSecurityRealm and create the build item
            RuntimeValue<SecurityRealm> realm = recorder.createRealm();
            securityRealm.produce(new SecurityRealmBuildItem(realm, realmConfig.realmName(), recorder.loadRealm(realm)));
            // Return the realm authentication mechanism build item
        }
    }

    @BuildStep
    void nativeResource(BuildProducer<NativeImageResourceBuildItem> resources) {
        if (propertiesConfig.file().enabled()) {
            PropertiesRealmConfig realmConfig = propertiesConfig.file();
            resources.produce(new NativeImageResourceBuildItem(realmConfig.users(), realmConfig.roles()));
        }
    }

    @BuildStep
    ElytronPasswordMarkerBuildItem marker() {
        if (propertiesConfig.file().enabled() || propertiesConfig.embedded().enabled()) {
            return new ElytronPasswordMarkerBuildItem();
        }
        return null;
    }

    /**
     * Check to see if the a MPRealmConfig was specified and enabled and create a
     * {@linkplain org.wildfly.security.auth.realm.SimpleMapBackedSecurityRealm}
     * runtime value.
     *
     * @param recorder - runtime security recorder
     * @param securityRealm - the producer factory for the SecurityRealmBuildItem
     * @return the AuthConfigBuildItem for the realm authentication mechanism if there was an enabled MPRealmConfig,
     *         null otherwise
     * @throws Exception - on any failure
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void configureMPRealmConfig(ElytronPropertiesFileRecorder recorder,
            BuildProducer<SecurityRealmBuildItem> securityRealm) throws Exception {
        if (propertiesConfig.embedded().enabled()) {
            log.info("Configuring from MPRealmConfig");
            RuntimeValue<SecurityRealm> realm = recorder.createEmbeddedRealm();
            securityRealm.produce(new SecurityRealmBuildItem(realm, propertiesConfig.embedded().realmName(),
                    recorder.loadEmbeddedRealm(realm)));
        }
    }
}
