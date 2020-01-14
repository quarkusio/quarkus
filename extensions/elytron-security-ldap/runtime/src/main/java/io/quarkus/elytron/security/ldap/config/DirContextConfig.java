package io.quarkus.elytron.security.ldap.config;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class DirContextConfig {

    /**
     * The url of the ldap server
     */
    @ConfigItem
    public String url;

    /**
     * The principal: user which is used to connect to ldap server (also named "bindDn")
     */
    @ConfigItem
    public String principal;

    /**
     * The password which belongs to the principal (also named "bindCredential")
     */
    @ConfigItem
    public String password;

    @Override
    public String toString() {
        return "DirContextConfig{" +
                "url='" + url + '\'' +
                ", principal='" + principal + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
