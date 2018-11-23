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

import java.io.FileInputStream;
import java.security.Permission;
import java.security.Provider;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Supplier;

import javax.servlet.ServletContext;

import org.jboss.shamrock.runtime.RuntimeValue;
import org.jboss.shamrock.runtime.Template;
import org.wildfly.security.WildFlyElytronProvider;
import org.wildfly.security.auth.realm.LegacyPropertiesSecurityRealm;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.authz.AuthorizationIdentity;
import org.wildfly.security.authz.PermissionMappable;
import org.wildfly.security.authz.PermissionMapper;
import org.wildfly.security.authz.RoleDecoder;
import org.wildfly.security.authz.Roles;
import org.wildfly.security.authz.SimpleAttributesEntry;
import org.wildfly.security.permission.PermissionVerifier;

import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.LoginConfig;
import io.undertow.servlet.api.ServletInfo;

@Template
public class SecurityTemplate {

    public void loadRealm(RuntimeValue<LegacyPropertiesSecurityRealm> realm, FileRealmConfig config) throws Exception {
        realm.getValue().load(new FileInputStream(config.users), new FileInputStream(config.roles));
    }

    public RuntimeValue<LegacyPropertiesSecurityRealm> createRealm() throws Exception {

        LegacyPropertiesSecurityRealm realm = LegacyPropertiesSecurityRealm.builder()
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


    public RuntimeValue<SecurityDomain> configureDomain(FileRealmConfig config, RuntimeValue<LegacyPropertiesSecurityRealm> realm) throws Exception {

        SecurityDomain domain = SecurityDomain.builder()
                .addRealm("default", realm.getValue())
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
                .setDefaultRealmName("default")
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
                .build();

        return new RuntimeValue<>(domain);
    }

    public ServletExtension configureUndertowIdentityManager(RuntimeValue<SecurityDomain> domain) {
        return new ServletExtension() {
            @Override
            public void handleDeployment(DeploymentInfo deploymentInfo, ServletContext servletContext) {
                //such hacks, this needs to be done properly, but is an example to get going
                boolean enable = false;
                for (ServletInfo i : deploymentInfo.getServlets().values()) {
                    if (i.getServletSecurityInfo() != null) {
                        enable = true;
                    }
                }
                if (enable) {
                    deploymentInfo.setLoginConfig(new LoginConfig("BASIC", "Shamrock"));
                }
                deploymentInfo.setIdentityManager(new ElytronIdentityManager(domain.getValue()));
            }
        };
    }

}
