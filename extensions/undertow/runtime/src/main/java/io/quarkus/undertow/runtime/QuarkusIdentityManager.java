package io.quarkus.undertow.runtime;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;

@Singleton
public class QuarkusIdentityManager implements IdentityManager {

    @Inject
    Logger log;

    @Inject
    IdentityProviderManager ipm;

    @Override
    public Account verify(final Account account) {
        log.debugf("verify1: %s", account.getPrincipal().getName());
        return account;
    }

    @Override
    public Account verify(final String id, final Credential credential) {
        log.debugf("verify2: %s - %s", id, credential.getClass());

        if (credential instanceof PasswordCredential) {
            final PasswordCredential password = PasswordCredential.class.cast(credential);
            final UsernamePasswordAuthenticationRequest upar = new UsernamePasswordAuthenticationRequest(id,
                    new io.quarkus.security.credential.PasswordCredential(password.getPassword()));
            return new QuarkusUndertowAccount(ipm.authenticateBlocking(upar));
        }

        return null;
    }

    @Override
    public Account verify(final Credential credential) {
        log.debugf("verify3: %s", credential.getClass());
        return null;
    }
}
