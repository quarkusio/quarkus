package io.quarkus.elytron.security.oauth2.runtime.auth;

import java.util.List;

import org.jboss.logging.Logger;
import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.evidence.BearerTokenEvidence;

import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;

public class OAuth2IdentityManager implements IdentityManager {
    private static Logger log = Logger.getLogger(OAuth2IdentityManager.class);
    private final SecurityDomain domain;
    private String roleClaim;

    public OAuth2IdentityManager(SecurityDomain domain, String roleClaim) {
        this.domain = domain;
        this.roleClaim = roleClaim;
    }

    @Override
    public Account verify(Account account) {
        return account;
    }

    @Override
    public Account verify(String id, Credential credential) {
        return null;
    }

    @Override
    public Account verify(Credential credential) {
        log.debugf("verify, credential=%s", credential);
        try {
            if (credential instanceof Oauth2Credential) {
                Oauth2Credential oauth2Credential = (Oauth2Credential) credential;

                try {
                    BearerTokenEvidence evidence = new BearerTokenEvidence(oauth2Credential.getBearerToken());
                    SecurityIdentity result = domain.authenticate(evidence);
                    String[] roles = extractRoles(result);

                    log.debugf("authenticate, result=%s", result);
                    if (result != null) {
                        return new OAuth2Account(result, roles);
                    }
                } catch (RealmUnavailableException e) {
                    log.debugf(e, "failed, credential=%s", credential);
                }
            }
        } catch (Exception e) {
            log.warnf(e, "Failed to verify credential=%s", credential);
        }
        return null;
    }

    private String[] extractRoles(SecurityIdentity result) {
        ElytronOAuth2CallerPrincipal principal = (ElytronOAuth2CallerPrincipal) result.getPrincipal();
        Object claims = principal.getClaims().get(roleClaim);
        if (claims instanceof List) {
            return ((List<String>) claims).toArray(new String[0]);
        }

        String claim = (String) principal.getClaims().get(roleClaim);
        if (claim == null) {
            return null;
        }
        return claim.split(" ");
    }
}
