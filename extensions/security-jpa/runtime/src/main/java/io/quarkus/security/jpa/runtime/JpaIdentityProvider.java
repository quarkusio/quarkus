package io.quarkus.security.jpa.runtime;

import java.util.function.Supplier;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.FlushMode;
import org.jboss.logging.Logger;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.smallrye.mutiny.Uni;

public abstract class JpaIdentityProvider extends AbstractJpaIdentityProvider
        implements IdentityProvider<UsernamePasswordAuthenticationRequest> {

    private static Logger log = Logger.getLogger(JpaIdentityProvider.class);

    @Inject
    EntityManagerFactory entityManagerFactory;

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
                EntityManager em = entityManagerFactory.createEntityManager();
                ((org.hibernate.Session) em).setHibernateFlushMode(FlushMode.MANUAL);
                ((org.hibernate.Session) em).setDefaultReadOnly(true);
                try {
                    return authenticate(em, request);
                } catch (SecurityException e) {
                    log.debug("Authentication failed", e);
                    throw new AuthenticationFailedException();
                } finally {
                    em.close();
                }
            }
        });
    }

    public abstract SecurityIdentity authenticate(EntityManager em,
            UsernamePasswordAuthenticationRequest request);

}
