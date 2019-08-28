package io.quarkus.smallrye.jwt.runtime;

import org.wildfly.security.auth.realm.token.TokenSecurityRealm;
import org.wildfly.security.auth.server.SecurityRealm;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.smallrye.jwt.runtime.auth.ElytronJwtCallerPrincipal;
import io.quarkus.smallrye.jwt.runtime.auth.JWTAuthMethodExtension;
import io.quarkus.smallrye.jwt.runtime.auth.MpJwtValidator;
import io.undertow.servlet.ServletExtension;

/**
 * The runtime value service used to create values related to the MP-JWT services
 */
@Recorder
public class SmallRyeJwtRecorder {

    /**
     * Create the JWTAuthMethodExtension servlet extension
     * 
     * @param authMechanism - name to use for MP-JWT auth mechanism
     * @param container - bean container to create JWTAuthMethodExtension bean
     * @return JWTAuthMethodExtension
     */
    public ServletExtension createAuthExtension(String authMechanism, BeanContainer container) {
        JWTAuthMethodExtension authExt = container.instance(JWTAuthMethodExtension.class);
        authExt.setAuthMechanism(authMechanism);
        return authExt;
    }

    /**
     * Create the TokenSecurityRealm
     * 
     * @return runtime wrapped TokenSecurityRealm
     */
    public RuntimeValue<SecurityRealm> createTokenRealm(BeanContainer container) {
        MpJwtValidator jwtValidator = container.instance(MpJwtValidator.class);
        TokenSecurityRealm tokenRealm = TokenSecurityRealm.builder()
                .claimToPrincipal(claims -> new ElytronJwtCallerPrincipal(claims))
                .validator(jwtValidator)
                .build();
        return new RuntimeValue<>(tokenRealm);
    }
}
