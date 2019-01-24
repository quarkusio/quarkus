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

package org.jboss.shamrock.security;

import java.net.URL;
import java.security.Permission;
import java.security.Provider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.servlet.ServletContext;

import org.jboss.logging.Logger;
import org.jboss.shamrock.runtime.RuntimeValue;
import org.jboss.shamrock.runtime.Template;
import org.wildfly.security.WildFlyElytronProvider;
import org.wildfly.security.auth.realm.LegacyPropertiesSecurityRealm;
import org.wildfly.security.auth.realm.SimpleMapBackedSecurityRealm;
import org.wildfly.security.auth.realm.SimpleRealmEntry;
import org.wildfly.security.auth.server.NameRewriter;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityRealm;
import org.wildfly.security.authz.Attributes;
import org.wildfly.security.authz.AuthorizationIdentity;
import org.wildfly.security.authz.MapAttributes;
import org.wildfly.security.authz.PermissionMappable;
import org.wildfly.security.authz.PermissionMapper;
import org.wildfly.security.authz.RoleDecoder;
import org.wildfly.security.authz.Roles;
import org.wildfly.security.authz.SimpleAttributesEntry;
import org.wildfly.security.credential.Credential;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.password.interfaces.ClearPassword;
import org.wildfly.security.permission.PermissionVerifier;

import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.LoginConfig;
import io.undertow.servlet.api.ServletInfo;

/**
 * The runtime security template class that provides methods for creating RuntimeValues for the deployment security objects.
 */
@Template
public class SecurityTemplate {
    static final Logger log = Logger.getLogger(SecurityTemplate.class);

    /**
     * Load the user.properties and roles.properties files into the {@linkplain SecurityRealm}
     * @param realm - a {@linkplain LegacyPropertiesSecurityRealm}
     * @param config - realm configuration info
     * @throws Exception
     */
    public void loadRealm(RuntimeValue<SecurityRealm> realm, PropertiesRealmConfig config) throws Exception {
        log.debugf("loadRealm, config=%s", config);
        SecurityRealm secRealm = realm.getValue();
        if(!(secRealm instanceof LegacyPropertiesSecurityRealm)) {
            return;
        }
        log.debugf("Trying to loader users: /%s", config.users);
        URL users = getClass().getResource("/" + config.users);
        log.debugf("users: %s", users);
        log.debugf("Trying to loader roles: %s", config.roles);
        URL roles = getClass().getResource("/" + config.roles);
        log.debugf("roles: %s", roles);
        if(users == null && roles == null) {
            String msg = String.format("No PropertiesRealmConfig users/roles settings found. Configure the shamrock.security.file.%s properties", config.help());
            throw new IllegalStateException(msg);
        }
        LegacyPropertiesSecurityRealm propsRealm = (LegacyPropertiesSecurityRealm) secRealm;
        propsRealm.load(users.openStream(), roles.openStream());
    }

    /**
     * Load the embedded user and role information into the {@linkplain SecurityRealm}
     * @param realm - a {@linkplain SimpleMapBackedSecurityRealm}
     * @param config - the realm config
     * @throws Exception
     */
    public void loadRealm(RuntimeValue<SecurityRealm> realm, MPRealmConfig config) throws Exception {
        log.debugf("loadRealm, config=%s", config);
        SecurityRealm secRealm = realm.getValue();
        if(!(secRealm instanceof SimpleMapBackedSecurityRealm)) {
            return;
        }
        SimpleMapBackedSecurityRealm memRealm = (SimpleMapBackedSecurityRealm) secRealm;
        HashMap<String, SimpleRealmEntry> identityMap = new HashMap<>();
        Map<String, String> userInfo = config.getUsers();
        log.debugf("UserInfoMap: %s\n", userInfo);
        Map<String, String> roleInfo = config.getRoles();
        log.debugf("RoleInfoMap: %s\n", roleInfo);
        for(String user : userInfo.keySet()) {
            String password = userInfo.get(user);
            ClearPassword clear = ClearPassword.createRaw(ClearPassword.ALGORITHM_CLEAR, password.toCharArray());
            PasswordCredential passwordCred = new PasswordCredential(clear);
            List<Credential> credentials = new ArrayList<>();
            credentials.add(passwordCred);
            String rawRoles = roleInfo.get(user);
            String[] roles = rawRoles.split(",");
            Attributes attributes = new MapAttributes();
            for(String role : roles) {
                attributes.addLast("groups", role);
            }
            SimpleRealmEntry entry = new SimpleRealmEntry(credentials, attributes);
            identityMap.put(user, entry);
            log.debugf("Added user(%s), roles=%s\n", user, attributes.get("groups"));
        }
        memRealm.setIdentityMap(identityMap);
    }

    /**
     * Create a runtime value for a {@linkplain LegacyPropertiesSecurityRealm}
     * @param config - the realm config
     * @return - runtime value wrapper for the SecurityRealm
     * @throws Exception
     */
    public RuntimeValue<SecurityRealm> createRealm(PropertiesRealmConfig config) throws Exception {
        log.debugf("createRealm, config=%s", config);

        SecurityRealm realm = LegacyPropertiesSecurityRealm.builder()
                .setDefaultRealm("default")
                .setProviders(new Supplier<Provider[]>() {
                    @Override
                    public Provider[] get() {
                        return new Provider[]{new WildFlyElytronProvider()};
                    }
                })
                .setPlainText(true)
                .build();
        return new RuntimeValue<>(realm);
    }
    /**
     * Create a runtime value for a {@linkplain SimpleMapBackedSecurityRealm}
     * @param config - the realm config
     * @return - runtime value wrapper for the SecurityRealm
     * @throws Exception
     */
    public RuntimeValue<SecurityRealm> createRealm(MPRealmConfig config) {
        log.debugf("createRealm, config=%s", config);

        Supplier<Provider[]> providers = new Supplier<Provider[]>() {
            @Override
            public Provider[] get() {
                return new Provider[]{new WildFlyElytronProvider()};
            }
        };
        SecurityRealm realm = new SimpleMapBackedSecurityRealm(NameRewriter.IDENTITY_REWRITER, providers);
        return new RuntimeValue<>(realm);
    }

    /**
     * Create a {@linkplain SecurityDomain.Builder} for the given default {@linkplain SecurityRealm}.
     * @param realmName - the default realm name
     * @param realm - the default SecurityRealm
     * @return a runtime value for the SecurityDomain.Builder
     * @throws Exception on any error
     */
    public RuntimeValue<SecurityDomain.Builder> configureDomainBuilder(String realmName, RuntimeValue<SecurityRealm> realm) throws Exception {
        log.debugf("buildDomain, realm=%s", realm.getValue());

        SecurityDomain.Builder domain = SecurityDomain.builder()
                .addRealm(realmName, realm.getValue())
                // Obtain the account roles from the groups attribute
                .setRoleDecoder(new RoleDecoder() {
                    @Override
                    public Roles decodeRoles(AuthorizationIdentity authorizationIdentity) {
                        SimpleAttributesEntry groups = (SimpleAttributesEntry) authorizationIdentity.getAttributes().get("groups");
                        Set<String> roles = new HashSet<>(groups);
                        return new Roles() {
                            @Override
                            public boolean contains(String roleName) {
                                return roles.contains(roleName);
                            }

                            @Override
                            public Iterator<String> iterator() {
                                return roles.iterator();
                            }
                        };
                    }
                })
                .build()
                .setDefaultRealmName(realmName)
                .setPermissionMapper(new PermissionMapper() {
                    @Override
                    public PermissionVerifier mapPermissions(PermissionMappable permissionMappable, Roles roles) {
                        return new PermissionVerifier() {
                            @Override
                            public boolean implies(Permission permission) {
                                return true;
                            }
                        };
                    }
                })
                ;

        return new RuntimeValue<>(domain);
    }

    /**
     * Called to add an additional realm to the {@linkplain SecurityDomain} being built
     * @param builder - runtime value for SecurityDomain.Builder created by {@linkplain #configureDomainBuilder(String, RuntimeValue)}
     * @param realmName - the name of the SecurityRealm
     * @param realm - the runtime value for the SecurityRealm
     */
    public void addRealm(RuntimeValue<SecurityDomain.Builder> builder, String realmName, RuntimeValue<SecurityRealm> realm) {
        builder.getValue().addRealm(realmName, realm.getValue());
    }

    /**
     * Called to invoke the builder created by {@linkplain #configureDomainBuilder(String, RuntimeValue)}
     * @param builder - the security domain builder
     * @return the security domain runtime value
     */
    public RuntimeValue<SecurityDomain> buildDomain(RuntimeValue<SecurityDomain.Builder> builder) {
        return new RuntimeValue<>(builder.getValue().build());
    }

    /**
     * Called to create a {@linkplain ServletExtension} to associate the {@linkplain ElytronIdentityManager} with the
     * deployment.
     *
     * @param domain - the SecurityDomain to use for auth decisions
     * @param authConfigs - the authenticaiton methods to register with the deployment {@linkplain LoginConfig}
     * @return - the ServletExtension instance to register
     */
    public ServletExtension configureUndertowIdentityManager(RuntimeValue<SecurityDomain> domain, List<AuthConfig> authConfigs) {
        return new ServletExtension() {
            @Override
            public void handleDeployment(DeploymentInfo deploymentInfo, ServletContext servletContext) {
                if (authConfigs.size() > 0) {
                    AuthConfig first = authConfigs.get(0);
                    log.debugf("configureUndertowIdentityManager, %s", authConfigs);
                    LoginConfig loginConfig = new LoginConfig(first.authMechanism, first.realmName);
                    for(int n = 1; n < authConfigs.size(); n ++) {
                        AuthConfig ac = authConfigs.get(n);
                        loginConfig.addLastAuthMethod(ac.getAuthMechanism());
                    }
                    deploymentInfo.setLoginConfig(loginConfig);
                }
                deploymentInfo.setIdentityManager(new ElytronIdentityManager(domain.getValue()));
            }
        };
    }

}
