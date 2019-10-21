package io.quarkus.elytron.security.properties.deployment;

import java.util.Set;

import org.jboss.logging.Logger;
import org.wildfly.security.auth.server.SecurityRealm;

import io.quarkus.deployment.QuarkusConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.elytron.security.deployment.ElytronPasswordMarkerBuildItem;
import io.quarkus.elytron.security.deployment.SecurityRealmBuildItem;
import io.quarkus.elytron.security.runtime.ElytronPropertiesFileRecorder;
import io.quarkus.elytron.security.runtime.MPRealmConfig;
import io.quarkus.elytron.security.runtime.PropertiesRealmConfig;
import io.quarkus.elytron.security.runtime.SecurityUsersConfig;
import io.quarkus.runtime.RuntimeValue;

/**
 * The build time process for the security aspects of the deployment. This creates {@linkplain BuildStep}s for integration
 * with the Elytron security services. This supports the Elytron
 * {@linkplain org.wildfly.security.auth.realm.LegacyPropertiesSecurityRealm}
 * and {@linkplain org.wildfly.security.auth.realm.SimpleMapBackedSecurityRealm} realm implementations. Others could be
 * added by creating an extension that produces a SecurityRealmBuildItem for the realm.
 *
 * Additional authentication mechanisms can be added by producing AuthConfigBuildItems and including the associated
 * {@linkplain io.undertow.servlet.ServletExtension} implementations to register the
 * {@linkplain io.undertow.security.api.AuthenticationMechanismFactory}.
 *
 *
 */
class ElytronPropertiesProcessor {
    private static final Logger log = Logger.getLogger(ElytronPropertiesProcessor.class.getName());
    /** Prefix for the user to password mapping properties */
    private static final String USERS_PREFIX = "quarkus.security.embedded.users";
    /** Prefix for the user to password mapping properties */
    private static final String ROLES_PREFIX = "quarkus.security.embedded.roles";

    SecurityUsersConfig propertiesConfig;

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.SECURITY_PROPERTIES_FILE);
    }

    /**
     * Check to see if a PropertiesRealmConfig was specified and enabled and create a
     * {@linkplain org.wildfly.security.auth.realm.LegacyPropertiesSecurityRealm}
     * runtime value to process the user/roles properties files. This also registers the names of the user/roles properties
     * files
     * to include the build artifact.
     *
     * @param recorder - runtime security recorder
     * @param resources - NativeImageResourceBuildItem used to register the realm user/roles properties files names.
     * @param securityRealm - the producer factory for the SecurityRealmBuildItem
     * @return the AuthConfigBuildItem for the realm authentication mechanism if there was an enabled PropertiesRealmConfig,
     *         null otherwise
     * @throws Exception - on any failure
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void configureFileRealmAuthConfig(ElytronPropertiesFileRecorder recorder,
            BuildProducer<NativeImageResourceBuildItem> resources,
            BuildProducer<SecurityRealmBuildItem> securityRealm) throws Exception {
        if (propertiesConfig.file.enabled) {
            PropertiesRealmConfig realmConfig = propertiesConfig.file;
            log.debugf("Configuring from PropertiesRealmConfig, users=%s, roles=%s", realmConfig.users,
                    realmConfig.roles);
            // Add the users/roles properties files resource names to build artifact
            resources.produce(new NativeImageResourceBuildItem(realmConfig.users, realmConfig.roles));
            // Have the runtime recorder create the LegacyPropertiesSecurityRealm and create the build item
            RuntimeValue<SecurityRealm> realm = recorder.createRealm(realmConfig);
            securityRealm
                    .produce(new SecurityRealmBuildItem(realm, realmConfig.realmName, recorder.loadRealm(realm, realmConfig)));
            // Return the realm authentication mechanism build item
        }
    }

    @BuildStep
    ElytronPasswordMarkerBuildItem marker() {
        if (propertiesConfig.file.enabled || propertiesConfig.embedded.enabled) {
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
    @Record(ExecutionTime.STATIC_INIT)
    void configureMPRealmConfig(ElytronPropertiesFileRecorder recorder,
            BuildProducer<SecurityRealmBuildItem> securityRealm) throws Exception {
        if (propertiesConfig.embedded.enabled) {
            MPRealmConfig realmConfig = propertiesConfig.embedded;
            log.info("Configuring from MPRealmConfig");
            // These are not being populated correctly by the core config Map logic for some reason, so reparse them here
            log.debugf("MPRealmConfig.users: %s", realmConfig.users);
            log.debugf("MPRealmConfig.roles: %s", realmConfig.roles);
            Set<String> userKeys = QuarkusConfig.getNames(USERS_PREFIX);

            log.debugf("userKeys: %s", userKeys);
            for (String key : userKeys) {
                String pass = QuarkusConfig.getString(USERS_PREFIX + '.' + key, null, false);
                log.debugf("%s.pass = %s", key, pass);
                realmConfig.users.put(key, pass);
            }
            Set<String> roleKeys = QuarkusConfig.getNames(ROLES_PREFIX);
            log.debugf("roleKeys: %s", roleKeys);
            for (String key : roleKeys) {
                String roles = QuarkusConfig.getString(ROLES_PREFIX + '.' + key, null, false);
                log.debugf("%s.roles = %s", key, roles);
                realmConfig.roles.put(key, roles);
            }

            RuntimeValue<SecurityRealm> realm = recorder.createRealm(realmConfig);
            securityRealm
                    .produce(new SecurityRealmBuildItem(realm, realmConfig.realmName, recorder.loadRealm(realm, realmConfig)));
        }
    }
}
