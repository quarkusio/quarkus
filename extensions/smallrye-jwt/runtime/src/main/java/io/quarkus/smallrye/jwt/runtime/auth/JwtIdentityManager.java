package io.quarkus.smallrye.jwt.runtime.auth;

import org.jboss.logging.Logger;
import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.evidence.BearerTokenEvidence;

import io.quarkus.security.ElytronAccount;
import io.smallrye.jwt.auth.principal.JWTCallerPrincipal;
import io.smallrye.jwt.auth.principal.JWTCallerPrincipalFactory;
import io.smallrye.jwt.auth.principal.ParseException;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;

public class JwtIdentityManager implements IdentityManager {
    private static Logger log = Logger.getLogger(JwtIdentityManager.class);
    private final SecurityDomain domain;

    public JwtIdentityManager(SecurityDomain domain) {
        this.domain = domain;
    }

    @Override
    public Account verify(Account account) {
        return account;
    }

    @Override
    public Account verify(String id, Credential credential) {
        log.debugf("verify, id=%s, credential=%s", id, credential);
        try {
            if (credential instanceof JWTCredential) {
                JWTCredential jwtCredential = (JWTCredential) credential;

                try {
                    BearerTokenEvidence evidence = new BearerTokenEvidence(jwtCredential.getBearerToken());
                    SecurityIdentity result = domain.authenticate(evidence);
                    log.debugf("authenticate, id=%s, result=%s", id, result);
                    if (result != null) {
                        return new ElytronAccount(result);
                    }
                } catch (RealmUnavailableException e) {
                    log.debugf(e, "failed, id=%s, credential=%s", id, credential);
                }
            }
        } catch (Exception e) {
            log.warnf(e, "Failed to verify id=%s", id);
        }
        return null;
    }

    @Override
    public Account verify(Credential credential) {
        return null;
    }

    /**
     * Validate the bearer token passed in with the authorization header
     *
     * @param jwtCredential - the input bearer token
     * @return return the validated JWTCallerPrincipal
     * @throws ParseException - thrown on token parse or validation failure
     */
    protected JWTCallerPrincipal validate(JWTCredential jwtCredential) throws ParseException {
        JWTCallerPrincipalFactory factory = JWTCallerPrincipalFactory.instance();
        JWTCallerPrincipal callerPrincipal = factory.parse(jwtCredential.getBearerToken(), jwtCredential.getAuthContextInfo());
        return callerPrincipal;
    }
}
