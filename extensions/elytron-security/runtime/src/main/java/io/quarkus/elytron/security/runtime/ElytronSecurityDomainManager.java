package io.quarkus.elytron.security.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

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
