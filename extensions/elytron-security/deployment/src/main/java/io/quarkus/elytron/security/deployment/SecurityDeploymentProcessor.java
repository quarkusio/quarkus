package io.quarkus.elytron.security.deployment;

import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
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
import io.quarkus.elytron.security.runtime.AuthConfig;
import io.quarkus.elytron.security.runtime.MPRealmConfig;
import io.quarkus.elytron.security.runtime.PropertiesRealmConfig;
import io.quarkus.elytron.security.runtime.SecurityConfig;
import io.quarkus.elytron.security.runtime.SecurityContextPrincipal;
import io.quarkus.elytron.security.runtime.SecurityRecorder;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.undertow.deployment.ServletExtensionBuildItem;
import io.undertow.security.idm.IdentityManager;
import io.undertow.servlet.ServletExtension;

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

    @BuildStep
    AdditionalBeanBuildItem registerAdditionalBeans() {
        return AdditionalBeanBuildItem.unremovableOf(SecurityContextPrincipal.class);
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
    AuthConfigBuildItem configureFileRealmAuthConfig(SecurityRecorder recorder,
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
            securityRealm.produce(new SecurityRealmBuildItem(realm, realmConfig.getAuthConfig()));
            passwordRealm.produce(new PasswordRealmBuildItem());
            // Return the realm authentication mechanism build item
            return new AuthConfigBuildItem(realmConfig.getAuthConfig());
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
    AuthConfigBuildItem configureMPRealmConfig(SecurityRecorder recorder,
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
            securityRealm.produce(new SecurityRealmBuildItem(realm, realmConfig.getAuthConfig()));
            passwordRealm.produce(new PasswordRealmBuildItem());
            return new AuthConfigBuildItem(realmConfig.getAuthConfig());
        }
        return null;
    }

    /**
     * Create the deployment SecurityDomain using the SecurityRealm and AuthConfig build items that have been created.
     *
     * @param recorder - the runtime recorder class used to access runtime behaviors
     * @param extension - the ServletExtensionBuildItem producer used to add the Undertow identity manager and auth config
     * @param realms - the previously created SecurityRealm runtime values
     * @return the SecurityDomain runtime value build item
     * @throws Exception
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    SecurityDomainBuildItem build(SecurityRecorder recorder, BuildProducer<ServletExtensionBuildItem> extension,
            List<SecurityRealmBuildItem> realms) throws Exception {
        log.debugf("build, hasFile=%s, hasMP=%s", security.file.enabled, security.embedded.enabled);
        if (realms.size() > 0) {
            // Configure the SecurityDomain.Builder from the main realm
            SecurityRealmBuildItem realmBuildItem = realms.get(0);
            AuthConfig authConfig = realmBuildItem.getAuthConfig();
            RuntimeValue<SecurityDomain.Builder> securityDomainBuilder = recorder
                    .configureDomainBuilder(authConfig.getRealmName(), realmBuildItem.getRealm());
            // Add any additional SecurityRealms
            for (int n = 1; n < realms.size(); n++) {
                realmBuildItem = realms.get(n);
                RuntimeValue<SecurityRealm> realm = realmBuildItem.getRealm();
                authConfig = realmBuildItem.getAuthConfig();
                recorder.addRealm(securityDomainBuilder, authConfig.getRealmName(), realm);
            }
            // Actually build the runtime value for the SecurityDomain
            RuntimeValue<SecurityDomain> securityDomain = recorder.buildDomain(securityDomainBuilder);

            // Return the build item for the SecurityDomain runtime value
            return new SecurityDomainBuildItem(securityDomain);
        }
        return null;
    }

    /**
     * If a password based realm was created, install the security extension
     * {@linkplain io.quarkus.elytron.security.runtime.ElytronIdentityManager}
     *
     * @param recorder - runtime recorder
     * @param securityDomain - configured SecurityDomain
     * @param identityManagerProducer - producer factory for IdentityManagerBuildItem
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void configureIdentityManager(SecurityRecorder recorder, SecurityDomainBuildItem securityDomain,
            BuildProducer<IdentityManagerBuildItem> identityManagerProducer,
            List<PasswordRealmBuildItem> passwordRealm) {
        if (passwordRealm.size() > 0) {
            IdentityManager identityManager = recorder.createIdentityManager(securityDomain.getSecurityDomain());
            identityManagerProducer.produce(new IdentityManagerBuildItem(identityManager));
        }
    }

    /**
     * Create the deployment SecurityDomain using the SecurityRealm and AuthConfig build items that have been created.
     *
     * @param recorder - the runtime recorder class used to access runtime behaviors
     * @param extension - the ServletExtensionBuildItem producer used to add the Undertow identity manager and auth config
     * @param authConfigs - the authentication method information that has been registered
     * @return the SecurityDomain runtime value build item
     * @throws Exception
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void addIdentityManager(SecurityRecorder recorder, BuildProducer<ServletExtensionBuildItem> extension,
            SecurityDomainBuildItem securityDomain, List<IdentityManagerBuildItem> identityManagers,
            List<AuthConfigBuildItem> authConfigs) {
        // If there are no identityManagers, exit
        if (identityManagers.size() == 0) {
            return;
        }

        // Validate that at most one IdentityManagerBuildItem was created
        if (identityManagers.size() > 1) {
            throw new IllegalStateException("Multiple IdentityManagerBuildItem seen: " + identityManagers);
        }
        // Create the configured identity manager
        IdentityManagerBuildItem identityManager = identityManagers.get(0);
        // Collect all of the authentication mechanisms and create a ServletExtension to register the Undertow identity manager
        ServletExtension idmExt = recorder.configureUndertowIdentityManager(securityDomain.getSecurityDomain(),
                identityManager.getIdentityManager());
        extension.produce(new ServletExtensionBuildItem(idmExt));
    }

    /**
     * Produces a {@code ServletExtension} to configure Undertow {@code AuthConfigBuildItem} produced during the build
     *
     * @param recorder - the runtime recorder class used to access runtime behaviors
     * @param extension - the ServletExtensionBuildItem producer used to add the Undertow auth config
     * @param authConfigs - the authentication method information that has been registered
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void addLoginConfig(SecurityRecorder recorder, List<AuthConfigBuildItem> authConfigs,
            BuildProducer<ServletExtensionBuildItem> extension) {
        List<AuthConfig> allAuthConfigs = new ArrayList<>();

        for (AuthConfigBuildItem authConfigExt : authConfigs) {
            AuthConfig ac = authConfigExt.getAuthConfig();
            allAuthConfigs.add(ac);
        }

        extension.produce(new ServletExtensionBuildItem(recorder.configureLoginConfig(allAuthConfigs)));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    ServletExtensionBuildItem addSecurityContextPrincipalHandler(SecurityRecorder recorder, BeanContainerBuildItem container) {
        log.debugf("addSecurityContextPrincipalHandler");
        return new ServletExtensionBuildItem(recorder.configureSecurityContextPrincipalHandler(container.getValue()));
    }

    /**
     * Register the classes for reflection in the requested named providers
     *
     * @param classes - ReflectiveClassBuildItem producer
     * @param jcaProviders - JCAProviderBuildItem for requested providers
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void registerJCAProviders(BuildProducer<ReflectiveClassBuildItem> classes, List<JCAProviderBuildItem> jcaProviders) {
        for (JCAProviderBuildItem provider : jcaProviders) {
            List<String> providerClasses = registerProvider(provider.getProviderName());
            for (String className : providerClasses) {
                classes.produce(new ReflectiveClassBuildItem(true, true, className));
                log.debugf("Register JCA class: %s", className);
            }
        }
    }

    /**
     * Determine the classes that make up the provider and its services
     *
     * @param providerName - JCA provider name
     * @return class names that make up the provider and its services
     */
    private List<String> registerProvider(String providerName) {
        ArrayList<String> providerClasses = new ArrayList<>();
        Provider provider = Security.getProvider(providerName);
        providerClasses.add(provider.getClass().getName());
        Set<Provider.Service> services = provider.getServices();
        for (Provider.Service service : services) {
            String serviceClass = service.getClassName();
            providerClasses.add(serviceClass);
            // Need to pull in the key classes
            String supportedKeyClasses = service.getAttribute("SupportedKeyClasses");
            if (supportedKeyClasses != null) {
                String[] keyClasses = supportedKeyClasses.split("\\|");
                providerClasses.addAll(Arrays.asList(keyClasses));
            }
        }
        return providerClasses;
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
    void loadRealm(SecurityRecorder recorder, List<SecurityRealmBuildItem> realms) throws Exception {
        for (SecurityRealmBuildItem realm : realms) {
            AuthConfig authConfig = realm.getAuthConfig();
            if (authConfig.getType() != null) {
                Class authType = authConfig.getType();
                if (authType.isAssignableFrom(PropertiesRealmConfig.class)) {
                    recorder.loadRealm(realm.getRealm(), security.file);
                } else if (authType.isAssignableFrom(MPRealmConfig.class)) {
                    recorder.loadRealm(realm.getRealm(), security.embedded);
                }
            }
        }
    }

}
