package io.quarkus.elytron.security.runtime;

import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;
import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.evidence.PasswordGuessEvidence;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

/**
 *
 * This is an interim class that provides a mapping between the existing Elytron implementations and the
 * new Quarkus API's.
 *
 */
@ApplicationScoped
public class ElytronPasswordIdentityProvider implements IdentityProvider<UsernamePasswordAuthenticationRequest> {

    private static Logger log = Logger.getLogger(ElytronPasswordIdentityProvider.class);

    @Inject
    SecurityDomain domain;

    @Override
    public Class<UsernamePasswordAuthenticationRequest> getRequestType() {
        return UsernamePasswordAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(UsernamePasswordAuthenticationRequest request,
            AuthenticationRequestContext context) {
        return context.runBlocking(new Supplier<SecurityIdentity>() {
            @Override
            public SecurityIdentity get() {
                org.wildfly.security.auth.server.SecurityIdentity result;
                try {
                    result = domain.authenticate(request.getUsername(),
                            new PasswordGuessEvidence(request.getPassword().getPassword()));

                    if (result == null) {
                        throw new AuthenticationFailedException();
                    }
                    QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
                    builder.setPrincipal(result.getPrincipal());
                    for (String i : result.getRoles()) {
                        builder.addRole(i);
                    }
                    builder.addCredential(request.getPassword());
                    return builder.build();
                } catch (RealmUnavailableException e) {
                    throw new RuntimeException(e);
                } catch (SecurityException e) {
                    log.debug("Authentication failed", e);
                    throw new AuthenticationFailedException(e);
                }
            }
        });
    }
}
