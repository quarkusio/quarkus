package io.quarkus.security.runtime;

import java.security.Principal;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;

import io.quarkus.arc.DefaultBean;
import io.quarkus.security.spi.runtime.AbstractSecurityIdentityAssociation;

@DefaultBean
@RequestScoped
public class SecurityIdentityAssociation extends AbstractSecurityIdentityAssociation {

    @DefaultBean
    @Produces
    @RequestScoped
    public Principal principal() {
        //TODO: as this is request scoped we loose the type of the Principal
        //if this is important you can just inject the identity
        return new Principal() {
            @Override
            public String getName() {
                return getIdentity().getPrincipal().getName();
            }
        };
    }

}
