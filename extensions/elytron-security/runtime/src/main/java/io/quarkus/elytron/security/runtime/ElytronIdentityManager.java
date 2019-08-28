package io.quarkus.elytron.security.runtime;

import javax.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;
import org.wildfly.security.auth.server.SecurityDomain;

import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;

/**
 * An implementation of the {@linkplain IdentityManager} that uses the Elytron {@linkplain SecurityDomain} associated with
 * the deployment
 */
public class ElytronIdentityManager implements IdentityManager {
    private static Logger log = Logger.getLogger(ElytronIdentityManager.class);

    @Override
    public Account verify(Account account) {
        return account;
    }

    @Override
    public Account verify(String id, Credential credential) {

        try {
            if (credential instanceof PasswordCredential) {
                IdentityProviderManager manager = CDI.current().select(IdentityProviderManager.class).get();
                SecurityIdentity securityIdentity = manager
                        .authenticateBlocking(new UsernamePasswordAuthenticationRequest(id,
                                new io.quarkus.security.credential.PasswordCredential(
                                        ((PasswordCredential) credential).getPassword())));
                return new QuarkusAccount(securityIdentity);
            }
        } catch (Exception e) {
            log.debugf(e, "Failed to authenticate");
        }
        return verify(credential);
    }

    @Override
    public Account verify(Credential credential) {
        if (credential instanceof UndertowTokenCredential) {

            IdentityProviderManager manager = CDI.current().select(IdentityProviderManager.class).get();
            SecurityIdentity securityIdentity = manager
                    .authenticateBlocking(new TokenAuthenticationRequest(
                            new TokenCredential(((UndertowTokenCredential) credential).getBearerToken(), "bearer")));
            return new QuarkusAccount(securityIdentity);
        }
        return null;
    }
}
