package io.quarkus.smallrye.jwt.runtime.auth;

import org.jboss.logging.Logger;
import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.evidence.BearerTokenEvidence;

import io.quarkus.elytron.security.runtime.ElytronAccount;
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
}
