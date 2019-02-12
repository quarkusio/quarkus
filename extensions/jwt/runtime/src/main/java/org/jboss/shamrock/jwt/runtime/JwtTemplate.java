package org.jboss.shamrock.jwt.runtime;

import java.security.Principal;

import io.undertow.security.idm.IdentityManager;
import io.undertow.servlet.ServletExtension;
import org.jboss.logging.Logger;
import org.jboss.shamrock.arc.runtime.BeanContainer;
import org.jboss.shamrock.jwt.runtime.auth.ElytronJwtCallerPrincipal;
import org.jboss.shamrock.jwt.runtime.auth.JWTAuthMethodExtension;
import org.jboss.shamrock.jwt.runtime.auth.JwtIdentityManager;
import org.jboss.shamrock.jwt.runtime.auth.MpJwtValidator;
import org.jboss.shamrock.runtime.RuntimeValue;
import org.jboss.shamrock.runtime.annotations.Template;
import org.wildfly.security.auth.realm.token.TokenSecurityRealm;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityRealm;
import org.wildfly.security.authz.Attributes;

/**
 * The runtime value service used to create values related to the MP-JWT services
 */
@Template
public class JwtTemplate {

    static final Logger log = Logger.getLogger(JwtTemplate.class);

    /**
     * Create the JwtIdentityManager
     * @param securityDomain - the SecurityDomain to use for auth decisions
     * @return - the IdentityManager instance to register
     */
    public IdentityManager createIdentityManager(RuntimeValue<SecurityDomain> securityDomain) {
        return new JwtIdentityManager(securityDomain.getValue());
    }

    /**
     * Create the JWTAuthMethodExtension servlet extension
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
     * @return runtime wrapped TokenSecurityRealm
     */
    public RuntimeValue<SecurityRealm> createTokenRealm(BeanContainer container) {
        MpJwtValidator jwtValidator = container.instance(MpJwtValidator.class);
        TokenSecurityRealm tokenRealm = TokenSecurityRealm.builder()
                .claimToPrincipal(this::mpJwtLogic)
                .validator(jwtValidator)
                .build();
        return new RuntimeValue<>(tokenRealm);
    }

    /**
     * MP-JWT logic for determining the name to use for the principal
     * @param claims - token claims
     * @return JWTCallerPrincipal implementation
     */
    private Principal mpJwtLogic(Attributes claims) {
        String pn = claims.getFirst("upn");
        if (pn == null) {
            pn = claims.getFirst("preferred_name");
        }
        if (pn == null) {
            pn = claims.getFirst("sub");
        }

        ElytronJwtCallerPrincipal jwtCallerPrincipal = new ElytronJwtCallerPrincipal(pn, claims);
        return jwtCallerPrincipal;
    }
}
