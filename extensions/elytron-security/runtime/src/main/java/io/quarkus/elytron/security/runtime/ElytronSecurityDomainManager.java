package io.quarkus.elytron.security.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.wildfly.security.auth.server.SecurityDomain;

@ApplicationScoped
public class ElytronSecurityDomainManager {

    private volatile SecurityDomain domain;

    @Produces
    public SecurityDomain getDomain() {
        return domain;
    }

    public ElytronSecurityDomainManager setDomain(SecurityDomain domain) {
        this.domain = domain;
        return this;
    }
}
