package org.jboss.shamrock.jwt.runtime.auth;

import org.jboss.shamrock.runtime.ObjectSubstitution;

public class JWTAuthMethodExtensionSubstitution implements ObjectSubstitution<JWTAuthMethodExtension, JWTAuthMethodExtensionProxy> {
    @Override
    public JWTAuthMethodExtensionProxy serialize(JWTAuthMethodExtension obj) {
        JWTAuthMethodExtensionProxy proxy = new JWTAuthMethodExtensionProxy();
        proxy.setAuthMechanism(obj.getAuthMechanism());
        return proxy;
    }

    @Override
    public JWTAuthMethodExtension deserialize(JWTAuthMethodExtensionProxy obj) {
        return new JWTAuthMethodExtension();
    }
}
