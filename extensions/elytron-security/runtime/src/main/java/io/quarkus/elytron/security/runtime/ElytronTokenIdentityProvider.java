package io.quarkus.elytron.security.runtime;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;
import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.evidence.BearerTokenEvidence;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;

/**
 *
 * This is an interim class that provides a mapping between the existing Elytron implementations and the
 * new Quarkus API's.
 *
 * This should be removed once we no longer depend on Elytron
 */
@ApplicationScoped
public class ElytronTokenIdentityProvider implements IdentityProvider<TokenAuthenticationRequest> {

    private static Logger log = Logger.getLogger(ElytronTokenIdentityProvider.class);

    @Inject
    SecurityDomain domain;

    @Override
    public Class<TokenAuthenticationRequest> getRequestType() {
        return TokenAuthenticationRequest.class;
    }

    @Override
    public CompletionStage<SecurityIdentity> authenticate(TokenAuthenticationRequest request,
            AuthenticationRequestContext context) {
        return context.runBlocking(new Supplier<SecurityIdentity>() {
            @Override
            public SecurityIdentity get() {
                org.wildfly.security.auth.server.SecurityIdentity result;
                try {
                    result = domain.authenticate(new BearerTokenEvidence(request.getToken().getToken()));

                    if (result == null) {
                        throw new AuthenticationFailedException();
                    }
                    QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
                    builder.setPrincipal(result.getPrincipal());
                    for (String i : result.getRoles()) {
                        builder.addRole(i);
                    }
                    builder.addCredential(request.getToken());
                    return builder.build();
                } catch (RealmUnavailableException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
