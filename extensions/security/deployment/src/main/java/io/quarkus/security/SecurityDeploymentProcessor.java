/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jboss.logging.Logger;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityRealm;

import io.quarkus.deployment.QuarkusConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.security.runtime.AuthConfig;
import io.quarkus.security.runtime.MPRealmConfig;
import io.quarkus.security.runtime.PropertiesRealmConfig;
import io.quarkus.security.runtime.SecurityConfig;
import io.quarkus.security.runtime.SecurityTemplate;
import io.quarkus.undertow.ServletExtensionBuildItem;
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
 * TODO: What additional features would be needed for Keycloak adaptor integration
 */
class SecurityDeploymentProcessor {
    private static final Logger log = Logger.getLogger(SecurityDeploymentProcessor.class.getName());

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
    void services(BuildProducer<ReflectiveClassBuildItem> classes) {
        classes.produce(
                new ReflectiveClassBuildItem(false, false, "org.wildfly.security.password.impl.PasswordFactorySpiImpl"));
    }

    /**
     * Check to see if a PropertiesRealmConfig was specified and enabled and create a
     * {@linkplain org.wildfly.security.auth.realm.LegacyPropertiesSecurityRealm}
     * runtime value to process the user/roles properties files. This also registers the names of the user/roles properties
     * files
     * to include the build artifact.
     *
     * @param template - runtime security template
     * @param resources - SubstrateResourceBuildItem used to register the realm user/roles properties files names.
     * @param securityRealm - the producer factory for the SecurityRealmBuildItem
     * @return the AuthConfigBuildItem for the realm authentication mechanism if there was an enabled PropertiesRealmConfig,
     *         null otherwise
     * @throws Exception - on any failure
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    AuthConfigBuildItem configureFileRealmAuthConfig(SecurityTemplate template,
            BuildProducer<SubstrateResourceBuildItem> resources,
            BuildProducer<SecurityRealmBuildItem> securityRealm,
            BuildProducer<PasswordRealmBuildItem> passwordRealm) throws Exception {
        if (security.file.enabled) {
            PropertiesRealmConfig realmConfig = security.file;
            log.debugf("Configuring from PropertiesRealmConfig, users=%s, roles=%s", realmConfig.getUsers(),
                    realmConfig.getRoles());
            // Add the users/roles properties files resource names to build artifact
            resources.produce(new SubstrateResourceBuildItem(realmConfig.users, realmConfig.roles));
            // Have the runtime template create the LegacyPropertiesSecurityRealm and create the build item
            RuntimeValue<SecurityRealm> realm = template.createRealm(realmConfig);
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
     * @param template - runtime security template
     * @param securityRealm - the producer factory for the SecurityRealmBuildItem
     * @return the AuthConfigBuildItem for the realm authentication mechanism if there was an enabled MPRealmConfig,
     *         null otherwise
     * @throws Exception - on any failure
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    AuthConfigBuildItem configureMPRealmConfig(SecurityTemplate template,
            BuildProducer<SecurityRealmBuildItem> securityRealm,
            BuildProducer<PasswordRealmBuildItem> passwordRealm) throws Exception {
        if (security.embedded.enabled) {
            MPRealmConfig realmConfig = security.embedded;
            log.info("Configuring from MPRealmConfig");
            // These are not being populated correctly by the core config Map logic for some reason, so reparse them here
            log.debugf("MPRealmConfig.users: %s", realmConfig.users);
            log.debugf("MPRealmConfig.roles: %s", realmConfig.roles);
            Set<String> userKeys = QuarkusConfig.getNames("quarkus.security.embedded.users");

            log.debugf("userKeys: %s", userKeys);
            for (String key : userKeys) {
                String pass = QuarkusConfig.getString("quarkus.security.embedded.users." + key, null, false);
                log.debugf("%s.pass = %s", key, pass);
                realmConfig.users.put(key, pass);
            }
            Set<String> roleKeys = QuarkusConfig.getNames("quarkus.security.embedded.roles");
            log.debugf("roleKeys: %s", roleKeys);
            for (String key : roleKeys) {
                String roles = QuarkusConfig.getString("quarkus.security.embedded.roles." + key, null, false);
                log.debugf("%s.roles = %s", key, roles);
                realmConfig.roles.put(key, roles);
            }

            RuntimeValue<SecurityRealm> realm = template.createRealm(realmConfig);
            securityRealm.produce(new SecurityRealmBuildItem(realm, realmConfig.getAuthConfig()));
            passwordRealm.produce(new PasswordRealmBuildItem());
            return new AuthConfigBuildItem(realmConfig.getAuthConfig());
        }
        return null;
    }

    /**
     * Create the deployment SecurityDomain using the SecurityRealm and AuthConfig build items that have been created.
     *
     * @param template - the runtime template class used to access runtime behaviors
     * @param extension - the ServletExtensionBuildItem producer used to add the Undertow identity manager and auth config
     * @param realms - the previously created SecurityRealm runtime values
     * @return the SecurityDomain runtime value build item
     * @throws Exception
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    SecurityDomainBuildItem build(SecurityTemplate template, BuildProducer<ServletExtensionBuildItem> extension,
            List<SecurityRealmBuildItem> realms) throws Exception {
        log.debugf("build, hasFile=%s, hasMP=%s", security.file.enabled, security.embedded.enabled);
        if (realms.size() > 0) {
            // Configure the SecurityDomain.Builder from the main realm
            SecurityRealmBuildItem realmBuildItem = realms.get(0);
            AuthConfig authConfig = realmBuildItem.getAuthConfig();
            RuntimeValue<SecurityDomain.Builder> securityDomainBuilder = template
                    .configureDomainBuilder(authConfig.getRealmName(), realmBuildItem.getRealm());
            // Add any additional SecurityRealms
            for (int n = 1; n < realms.size(); n++) {
                realmBuildItem = realms.get(n);
                RuntimeValue<SecurityRealm> realm = realmBuildItem.getRealm();
                authConfig = realmBuildItem.getAuthConfig();
                template.addRealm(securityDomainBuilder, authConfig.getRealmName(), realm);
            }
            // Actually build the runtime value for the SecurityDomain
            RuntimeValue<SecurityDomain> securityDomain = template.buildDomain(securityDomainBuilder);

            // Return the build item for the SecurityDomain runtime value
            return new SecurityDomainBuildItem(securityDomain);
        }
        return null;
    }

    /**
     * If a password based realm was created, install the security extension
     * {@linkplain io.quarkus.security.runtime.ElytronIdentityManager}
     *
     * @param template - runtime template
     * @param securityDomain - configured SecurityDomain
     * @param identityManagerProducer - producer factory for IdentityManagerBuildItem
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void configureIdentityManager(SecurityTemplate template, SecurityDomainBuildItem securityDomain,
            BuildProducer<IdentityManagerBuildItem> identityManagerProducer,
            List<PasswordRealmBuildItem> passwordRealm) {
        if (passwordRealm.size() > 0) {
            IdentityManager identityManager = template.createIdentityManager(securityDomain.getSecurityDomain());
            identityManagerProducer.produce(new IdentityManagerBuildItem(identityManager));
        }
    }

    /**
     * Create the deployment SecurityDomain using the SecurityRealm and AuthConfig build items that have been created.
     *
     * @param template - the runtime template class used to access runtime behaviors
     * @param extension - the ServletExtensionBuildItem producer used to add the Undertow identity manager and auth config
     * @param authConfigs - the authentication method information that has been registered
     * @return the SecurityDomain runtime value build item
     * @throws Exception
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void addIdentityManager(SecurityTemplate template, BuildProducer<ServletExtensionBuildItem> extension,
            SecurityDomainBuildItem securityDomain, List<IdentityManagerBuildItem> identityManagers,
            List<AuthConfigBuildItem> authConfigs) {
        // Validate the at most one IdentityManagerBuildItem was created
        if (identityManagers.size() > 1) {
            throw new IllegalStateException("Multiple IdentityManagerBuildItem seen: " + identityManagers);
        }
        // Only create an identityManager if one was configured
        if (identityManagers.size() > 0) {
            IdentityManagerBuildItem identityManager = identityManagers.get(0);
            // Collect all of the authentication mechanisms and create a ServletExtension to register the Undertow identity manager
            ArrayList<AuthConfig> allAuthConfigs = new ArrayList<>();
            for (AuthConfigBuildItem authConfigExt : authConfigs) {
                AuthConfig ac = authConfigExt.getAuthConfig();
                allAuthConfigs.add(ac);
            }
            ServletExtension idmExt = template.configureUndertowIdentityManager(securityDomain.getSecurityDomain(),
                    identityManager.getIdentityManager(), allAuthConfigs);
            extension.produce(new ServletExtensionBuildItem(idmExt));
        }
    }

    /**
     * For each SecurityRealm, load it's runtime state. This is currently a little strange due to how the AuthConfig is
     * downcast to the type of SecurityRealm configuration instance.
     *
     * @param template - the runtime template class used to access runtime behaviors
     * @param realms - the previously created SecurityRealm runtime values
     * @throws Exception
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void loadRealm(SecurityTemplate template, List<SecurityRealmBuildItem> realms) throws Exception {
        for (SecurityRealmBuildItem realm : realms) {
            AuthConfig authConfig = realm.getAuthConfig();
            if (authConfig.getType() != null) {
                Class authType = authConfig.getType();
                if (authType.isAssignableFrom(PropertiesRealmConfig.class)) {
                    template.loadRealm(realm.getRealm(), security.file);
                } else if (authType.isAssignableFrom(MPRealmConfig.class)) {
                    template.loadRealm(realm.getRealm(), security.embedded);
                }
            }
        }
    }

}
