package io.quarkus.elytron.security.properties.deployment;

import org.jboss.logging.Logger;
import org.wildfly.security.auth.server.SecurityRealm;

import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.elytron.security.deployment.ElytronPasswordMarkerBuildItem;
import io.quarkus.elytron.security.properties.runtime.ElytronPropertiesFileRecorder;
import io.quarkus.elytron.security.properties.runtime.MPRealmRuntimeConfig;
import io.quarkus.elytron.security.properties.runtime.PropertiesRealmConfig;
import io.quarkus.elytron.security.properties.runtime.SecurityUsersConfig;

/**
 * The build time process for the security aspects of the deployment. This creates {@linkplain BuildStep}s for integration
 * with the Elytron security services. This supports the Elytron
 * {@linkplain org.wildfly.security.auth.realm.LegacyPropertiesSecurityRealm}
 * and {@linkplain org.wildfly.security.auth.realm.SimpleMapBackedSecurityRealm} realm implementations.
 */
class ElytronPropertiesProcessor {
    private static final Logger log = Logger.getLogger(ElytronPropertiesProcessor.class.getName());

    SecurityUsersConfig propertiesConfig;

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.SECURITY_PROPERTIES_FILE);
    }

    /**
     * Check to see if a PropertiesRealmConfig was specified and enabled and register a
     * {@linkplain org.wildfly.security.auth.realm.LegacyPropertiesSecurityRealm} service that loads the users and roles
     * properties files.
     */
    @BuildStep
    void configureFileRealmAuthConfig(ServiceRegistrar serviceRegistrar) {
        if (propertiesConfig.file().enabled()) {
            PropertiesRealmConfig realmConfig = propertiesConfig.file();
            log.debugf("Configuring from PropertiesRealmConfig, users=%s, roles=%s", realmConfig.users(),
                    realmConfig.roles());
            // build-and-run-time-fixed config must be captured, use a local variable so that this processor is not captured
            SecurityUsersConfig buildTimeConfig = propertiesConfig;
            serviceRegistrar.forService(SecurityRealm.class, realmConfig.realmName())
                    .atPhase(Phase.DATA)
                    .onStart(ctx -> ElytronPropertiesFileRecorder.createRealm(buildTimeConfig));
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
     * Check to see if the MPRealmConfig was specified and enabled and create a
     * {@linkplain org.wildfly.security.auth.realm.SimpleMapBackedSecurityRealm}.
     */
    @BuildStep
    void configureMPRealmConfig(ServiceRegistrar serviceRegistrar) {
        if (propertiesConfig.embedded().enabled()) {
            log.info("Configuring from MPRealmConfig");
            // build-and-run-time-fixed config must be captured, use a local variable so that this processor is not captured
            SecurityUsersConfig buildTimeConfig = propertiesConfig;
            serviceRegistrar.forService(SecurityRealm.class, propertiesConfig.embedded().realmName())
                    .atPhase(Phase.DATA)
                    .require(MPRealmRuntimeConfig.class)
                    .onStart((ctx, runtimeConfig) -> ElytronPropertiesFileRecorder
                            .createEmbeddedRealm(buildTimeConfig, runtimeConfig));
        }
    }
}
