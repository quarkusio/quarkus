package io.quarkus.security.runtime;

import java.security.Principal;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;

import io.quarkus.arc.DefaultBean;
import io.quarkus.security.identity.CurrentIdentityAssociation;

public class PrincipalProducer {

    @DefaultBean
    @Produces
    @RequestScoped
    public Principal principal(CurrentIdentityAssociation identityAssociation) {
        //TODO: as this is request scoped we loose the type of the Principal
        //if this is important you can just inject the identity
        return new Principal() {
            @Override
            public String getName() {
                return identityAssociation.getIdentity().getPrincipal().getName();
            }
        };
    }

}
