package io.quarkus.elytron.security.oauth2.runtime.auth;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.wildfly.security.auth.server.SecurityIdentity;

import io.quarkus.elytron.security.runtime.ElytronAccount;

public class OAuth2Account extends ElytronAccount {
    private Set<String> roles = new HashSet<>();

    public OAuth2Account(SecurityIdentity securityIdentity) {
        super(securityIdentity);

        for (String i : securityIdentity.getRoles()) {
            roles.add(i);
        }
    }

    public OAuth2Account(SecurityIdentity securityIdentity, String[] roles) {
        super(securityIdentity);

        this.roles.addAll(Arrays.asList(roles));
    }

    @Override
    public Set<String> getRoles() {
        return roles;
    }
}
