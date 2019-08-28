package io.quarkus.elytron.security.runtime;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import io.quarkus.security.identity.SecurityIdentity;
import io.undertow.security.idm.Account;

/**
 * An Undertow account implementation that maps to the Elytron {@link SecurityIdentity}
 */
public class QuarkusAccount implements Account {

    private final SecurityIdentity securityIdentity;

    public QuarkusAccount(SecurityIdentity securityIdentity) {
        this.securityIdentity = securityIdentity;
    }

    @Override
    public Principal getPrincipal() {
        return securityIdentity.getPrincipal();
    }

    @Override
    public Set<String> getRoles() {
        Set<String> roles = new HashSet<>();
        for (String i : securityIdentity.getRoles()) {
            roles.add(i);
        }
        return roles;
    }

    public SecurityIdentity getSecurityIdentity() {
        return securityIdentity;
    }
}
