package org.jboss.shamrock.jwt.runtime;

import java.security.Principal;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.undertow.security.idm.IdentityManager;
import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.LoginConfig;
import org.jboss.logging.Logger;
import org.jboss.shamrock.jwt.runtime.auth.ElytronJwtCallerPrincipal;
import org.jboss.shamrock.jwt.runtime.auth.JWTAuthMethodExtension;
import org.jboss.shamrock.jwt.runtime.auth.JwtIdentifyManager;
import org.jboss.shamrock.jwt.runtime.auth.MpJwtValidator;
import org.jboss.shamrock.runtime.RuntimeValue;
import org.jboss.shamrock.runtime.Template;
import org.wildfly.security.auth.realm.token.TokenSecurityRealm;
import org.wildfly.security.auth.realm.token.validator.JwtValidator;
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
     *
     * @param securityDomain - the SecurityDomain to use for auth decisions
     * @return - the IdentityManager instance to register
     */
    public RuntimeValue<IdentityManager> createIdentityManager(RuntimeValue<SecurityDomain> securityDomain) {
        return new RuntimeValue<>(new JwtIdentifyManager(securityDomain.getValue()));
    }

    public RuntimeValue<JWTAuthMethodExtension> createAuthExtension(String authMechanism, JWTAuthContextInfo contextInfo) {
        return new RuntimeValue<>(new JWTAuthMethodExtension(authMechanism, contextInfo));
    }

    /**
     *
     * @return
     */
    public RuntimeValue<SecurityRealm> createTokenRealm(JWTAuthContextInfo contextInfo) {
        MpJwtValidator jwtValidator = new MpJwtValidator(contextInfo);
        TokenSecurityRealm tokenRealm = TokenSecurityRealm.builder()
                .claimToPrincipal(this::mpJwtLogic)
                .validator(jwtValidator)
                .build();
        return new RuntimeValue<>(tokenRealm);
    }

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
