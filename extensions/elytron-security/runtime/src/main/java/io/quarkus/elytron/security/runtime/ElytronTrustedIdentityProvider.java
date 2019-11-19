package io.quarkus.elytron.security.runtime;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;
import org.wildfly.security.auth.server.RealmIdentity;
import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.ServerAuthenticationContext;
import org.wildfly.security.credential.PasswordCredential;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.TrustedAuthenticationRequest;

/**
 *
 *
 */
@ApplicationScoped
public class ElytronTrustedIdentityProvider implements IdentityProvider<TrustedAuthenticationRequest> {

    private static Logger log = Logger.getLogger(ElytronTrustedIdentityProvider.class);

    @Inject
    SecurityDomain domain;

    @Override
    public Class<TrustedAuthenticationRequest> getRequestType() {
        return TrustedAuthenticationRequest.class;
    }

    @Override
    public CompletionStage<SecurityIdentity> authenticate(TrustedAuthenticationRequest request,
            AuthenticationRequestContext context) {
        return context.runBlocking(new Supplier<SecurityIdentity>() {
            @Override
            public SecurityIdentity get() {
                org.wildfly.security.auth.server.SecurityIdentity result;
                try {
                    RealmIdentity id = domain.getIdentity(request.getPrincipal());
                    if (!id.exists()) {
                        return null;
                    }
                    PasswordCredential cred = id.getCredential(PasswordCredential.class);
                    try (ServerAuthenticationContext ac = domain.createNewAuthenticationContext()) {
                        ac.setAuthenticationName(request.getPrincipal());
                        ac.addPrivateCredential(cred);
                        ac.authorize();
                        result = ac.getAuthorizedIdentity();

                        if (result == null) {
                            throw new AuthenticationFailedException();
                        }
                        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
                        builder.setPrincipal(result.getPrincipal());
                        for (String i : result.getRoles()) {
                            builder.addRole(i);
                        }
                        return builder.build();
                    }
                } catch (RealmUnavailableException e) {
                    throw new RuntimeException(e);
                } catch (SecurityException e) {
                    log.debug("Authentication failed", e);
                    throw new AuthenticationFailedException();
                }
            }
        });
    }
}
