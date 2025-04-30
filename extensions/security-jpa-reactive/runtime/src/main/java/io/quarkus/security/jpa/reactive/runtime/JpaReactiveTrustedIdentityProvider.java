package io.quarkus.security.jpa.reactive.runtime;

import java.util.function.Function;
import java.util.function.Predicate;

import jakarta.inject.Inject;
import jakarta.persistence.NonUniqueResultException;

import org.hibernate.FlushMode;
import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.logging.Logger;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TrustedAuthenticationRequest;
import io.smallrye.mutiny.Uni;

public abstract class JpaReactiveTrustedIdentityProvider implements IdentityProvider<TrustedAuthenticationRequest> {

    private static final Logger LOG = Logger.getLogger(JpaReactiveTrustedIdentityProvider.class);

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Override
    public Class<TrustedAuthenticationRequest> getRequestType() {
        return TrustedAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(TrustedAuthenticationRequest request,
            AuthenticationRequestContext authenticationRequestContext) {
        return sessionFactory.withSession(new Function<Mutiny.Session, Uni<SecurityIdentity>>() {
            @Override
            public Uni<SecurityIdentity> apply(Mutiny.Session session) {
                session.setFlushMode(FlushMode.MANUAL);
                session.setDefaultReadOnly(true);
                return authenticate(session, request)
                        .onFailure(new Predicate<Throwable>() {
                            @Override
                            public boolean test(Throwable throwable) {
                                return throwable instanceof SecurityException || throwable instanceof NonUniqueResultException;
                            }
                        })
                        .transform(new Function<Throwable, Throwable>() {
                            @Override
                            public Throwable apply(Throwable throwable) {
                                LOG.debug("Authentication failed", throwable);
                                return new AuthenticationFailedException(throwable);
                            }
                        });
            }
        });
    }

    public abstract Uni<SecurityIdentity> authenticate(Mutiny.Session session, TrustedAuthenticationRequest request);

}
