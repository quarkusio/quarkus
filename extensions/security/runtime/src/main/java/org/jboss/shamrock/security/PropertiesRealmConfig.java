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

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * A configuration object for a properties resource based realm configuration,
 * {@linkplain org.wildfly.security.auth.realm.LegacyPropertiesSecurityRealm}
 * It consists of a users.properties that has the format:
 * user1=password1
 * user2=password2
 *
 * and a roles.properties that has the format:
 * user1=role1,role2,...,roleN1
 * user2=role21,role2,...,roleN2
 */
@ConfigGroup
public class PropertiesRealmConfig {

    /**
     * The authentication mechanism
     */
    @ConfigItem(defaultValue = "BASIC")
    public String authMechanism;

    /**
     * The authentication mechanism
     */
    @ConfigItem(defaultValue = "Quarkus")
    public String realmName;

    /**
     * If the properties store is enabled.
     */
    @ConfigItem
    public boolean enabled;

    /**
     * The location of the users property resource
     */
    @ConfigItem(defaultValue = "users.properties")
    public String users;

    /**
     * The location of the roles property file
     */
    @ConfigItem(defaultValue = "roles.properties")
    public String roles;

    public String help() {
        return "{enabled,users,roles,authMechanism,realmName}";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUsers() {
        return users;
    }

    public void setUsers(String users) {
        this.users = users;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public String getAuthMechanism() {
        return authMechanism;
    }

    public void setAuthMechanism(String authMechanism) {
        this.authMechanism = authMechanism;
    }

    public String getRealmName() {
        return realmName;
    }

    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    /**
     * Used to access what should be a parent class, but parsing of the MP config properties is not working
     * from parent to child
     * 
     * @return AuthConfig information
     */
    public AuthConfig getAuthConfig() {
        return new AuthConfig(authMechanism, realmName, getClass());
    }

    @Override
    public String toString() {
        return "PropertiesRealmConfig{" +
                "authMechanism='" + authMechanism + '\'' +
                ", realmName='" + realmName + '\'' +
                ", enabled=" + enabled +
                ", users='" + users + '\'' +
                ", roles='" + roles + '\'' +
                '}';
    }
}
