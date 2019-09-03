package io.quarkus.elytron.security.deployment;

import java.util.List;
import java.util.Set;

import org.jboss.logging.Logger;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityRealm;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.QuarkusConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.elytron.security.runtime.ElytronPasswordIdentityProvider;
import io.quarkus.elytron.security.runtime.ElytronRecorder;
import io.quarkus.elytron.security.runtime.ElytronSecurityDomainManager;
import io.quarkus.elytron.security.runtime.ElytronTokenIdentityProvider;
import io.quarkus.elytron.security.runtime.MPRealmConfig;
import io.quarkus.elytron.security.runtime.PropertiesRealmConfig;
import io.quarkus.elytron.security.runtime.SecurityConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.security.deployment.JCAProviderBuildItem;

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
 * TODO: The handling of the configuration to SecurityRealm instance creation/loading is clumsy to not being able to
 * have a config object annotated with @ConfigGroup inherit from another object with MP config annotated properties.
 *
 */
class SecurityDeploymentProcessor {
    private static final Logger log = Logger.getLogger(SecurityDeploymentProcessor.class.getName());
    /** Prefix for the user to password mapping properties */
    private static final String USERS_PREFIX = "quarkus.security.embedded.users";
    /** Prefix for the user to password mapping properties */
    private static final String ROLES_PREFIX = "quarkus.security.embedded.roles";

    SecurityConfig security;

    /**
     * Register this extension as a MP-JWT feature
     *
     * @return
     */
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.SECURITY);
    }

    /**
     * Register the Elytron-provided password factory SPI implementation
     *
     * @param classes producer factory for ReflectiveClassBuildItems
     */
    @BuildStep
    void services(BuildProducer<ReflectiveClassBuildItem> classes, BuildProducer<JCAProviderBuildItem> jcaProviders) {
        String[] allClasses = {
                "org.wildfly.security.password.impl.PasswordFactorySpiImpl",
        };
        classes.produce(new ReflectiveClassBuildItem(true, false, allClasses));

        // Create JCAProviderBuildItems for any configured provider names
        if (security.securityProviders != null) {
            for (String providerName : security.securityProviders) {
                jcaProviders.produce(new JCAProviderBuildItem(providerName));
                log.debugf("Added providerName: %s", providerName);
            }
        }
    }

    /**
     * Check to see if a PropertiesRealmConfig was specified and enabled and create a
     * {@linkplain org.wildfly.security.auth.realm.LegacyPropertiesSecurityRealm}
     * runtime value to process the user/roles properties files. This also registers the names of the user/roles properties
     * files
     * to include the build artifact.
     *
     * @param recorder - runtime security recorder
     * @param resources - SubstrateResourceBuildItem used to register the realm user/roles properties files names.
     * @param securityRealm - the producer factory for the SecurityRealmBuildItem
     * @return the AuthConfigBuildItem for the realm authentication mechanism if there was an enabled PropertiesRealmConfig,
     *         null otherwise
     * @throws Exception - on any failure
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void configureFileRealmAuthConfig(ElytronRecorder recorder,
            BuildProducer<SubstrateResourceBuildItem> resources,
            BuildProducer<SecurityRealmBuildItem> securityRealm,
            BuildProducer<PasswordRealmBuildItem> passwordRealm) throws Exception {
        if (security.file.enabled) {
            PropertiesRealmConfig realmConfig = security.file;
            log.debugf("Configuring from PropertiesRealmConfig, users=%s, roles=%s", realmConfig.getUsers(),
                    realmConfig.getRoles());
            // Add the users/roles properties files resource names to build artifact
            resources.produce(new SubstrateResourceBuildItem(realmConfig.users, realmConfig.roles));
            // Have the runtime recorder create the LegacyPropertiesSecurityRealm and create the build item
            RuntimeValue<SecurityRealm> realm = recorder.createRealm(realmConfig);
            securityRealm
                    .produce(new SecurityRealmBuildItem(realm, realmConfig.realmName, recorder.loadRealm(realm, realmConfig)));
            passwordRealm.produce(new PasswordRealmBuildItem());
            // Return the realm authentication mechanism build item
        }
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
    void configureMPRealmConfig(ElytronRecorder recorder,
            BuildProducer<SecurityRealmBuildItem> securityRealm,
            BuildProducer<PasswordRealmBuildItem> passwordRealm) throws Exception {
        if (security.embedded.enabled) {
            MPRealmConfig realmConfig = security.embedded;
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
            passwordRealm.produce(new PasswordRealmBuildItem());
        }
    }

    /**
     * Create the deployment SecurityDomain using the SecurityRealm and AuthConfig build items that have been created.
     *
     * @param recorder - the runtime recorder class used to access runtime behaviors
     * @param realms - the previously created SecurityRealm runtime values
     * @return the SecurityDomain runtime value build item
     * @throws Exception
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    SecurityDomainBuildItem build(ElytronRecorder recorder, List<SecurityRealmBuildItem> realms,
            BuildProducer<AdditionalBeanBuildItem> beans)
            throws Exception {
        log.debugf("build, hasFile=%s, hasMP=%s", security.file.enabled, security.embedded.enabled);
        if (realms.size() > 0) {

            beans.produce(AdditionalBeanBuildItem.unremovableOf(ElytronSecurityDomainManager.class));
            beans.produce(AdditionalBeanBuildItem.unremovableOf(ElytronTokenIdentityProvider.class));
            beans.produce(AdditionalBeanBuildItem.unremovableOf(ElytronPasswordIdentityProvider.class));

            // Configure the SecurityDomain.Builder from the main realm
            SecurityRealmBuildItem realmBuildItem = realms.get(0);
            RuntimeValue<SecurityDomain.Builder> securityDomainBuilder = recorder
                    .configureDomainBuilder(realmBuildItem.getName(), realmBuildItem.getRealm());
            // Add any additional SecurityRealms
            for (int n = 1; n < realms.size(); n++) {
                realmBuildItem = realms.get(n);
                RuntimeValue<SecurityRealm> realm = realmBuildItem.getRealm();
                recorder.addRealm(securityDomainBuilder, realmBuildItem.getName(), realm);
            }
            // Actually build the runtime value for the SecurityDomain
            RuntimeValue<SecurityDomain> securityDomain = recorder.buildDomain(securityDomainBuilder);

            // Return the build item for the SecurityDomain runtime value
            return new SecurityDomainBuildItem(securityDomain);
        }
        return null;
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void identityManager(ElytronRecorder recorder, SecurityDomainBuildItem securityDomain, BeanContainerBuildItem bc) {
        if (securityDomain != null) {
            recorder.setDomainForIdentityProvider(bc.getValue(), securityDomain.getSecurityDomain());
        }
    }

    /**
     * For each SecurityRealm, load it's runtime state. This is currently a little strange due to how the AuthConfig is
     * downcast to the type of SecurityRealm configuration instance.
     *
     * @param recorder - the runtime recorder class used to access runtime behaviors
     * @param realms - the previously created SecurityRealm runtime values
     * @throws Exception
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void loadRealm(ElytronRecorder recorder, List<SecurityRealmBuildItem> realms) throws Exception {
        for (SecurityRealmBuildItem realm : realms) {
            if (realm.getRuntimeLoadTask() != null) {
                recorder.runLoadTask(realm.getRuntimeLoadTask());
            }
        }
    }

}
