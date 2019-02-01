package org.jboss.shamrock.jwt.runtime.auth;

import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;

public class JWTAuthMethodExtensionProxy {
    private String authMechanism;
    private JWTAuthContextInfo contextInfo;

    public String getAuthMechanism() {
        return authMechanism;
    }

    public void setAuthMechanism(String authMechanism) {
        this.authMechanism = authMechanism;
    }

    public JWTAuthContextInfo getContextInfo() {
        return contextInfo;
    }

    public void setContextInfo(JWTAuthContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }
}
