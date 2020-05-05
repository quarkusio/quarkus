package io.quarkus.security.jpa.runtime;

import java.util.function.Supplier;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.hibernate.FlushMode;
import org.jboss.logging.Logger;

import io.quarkus.hibernate.orm.runtime.JPAConfig;
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
    JPAConfig jpaConfig;

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
                // FIXME: unit name
                EntityManager em = jpaConfig.getEntityManagerFactory(null).createEntityManager();
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
