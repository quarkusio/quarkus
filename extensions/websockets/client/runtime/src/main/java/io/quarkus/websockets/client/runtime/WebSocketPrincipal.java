package io.quarkus.websockets.client.runtime;

import java.security.Principal;

import io.quarkus.security.identity.SecurityIdentity;

public class WebSocketPrincipal implements Principal {

    final SecurityIdentity securityIdentity;

    public WebSocketPrincipal(SecurityIdentity securityIdentity) {
        this.securityIdentity = securityIdentity;
    }

    @Override
    public String getName() {
        return securityIdentity.getPrincipal().getName();
    }

    public SecurityIdentity getSecurityIdentity() {
        return securityIdentity;
    }

}
