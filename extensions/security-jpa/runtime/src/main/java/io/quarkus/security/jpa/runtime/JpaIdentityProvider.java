package io.quarkus.security.jpa.runtime;

import java.util.List;
import java.util.function.Supplier;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.jpa.common.runtime.JpaIdentityProviderUtil;
import io.smallrye.mutiny.Uni;

public abstract class JpaIdentityProvider implements IdentityProvider<UsernamePasswordAuthenticationRequest> {

    private static Logger log = Logger.getLogger(JpaIdentityProvider.class);

    @Inject
    SessionFactory sessionFactory;

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
                if (requireActiveCDIRequestContext() && !Arc.container().requestContext().isActive()) {
                    var requestContext = Arc.container().requestContext();
                    requestContext.activate();
                    try {
                        return authenticate(request);
                    } finally {
                        requestContext.terminate();
                    }
                }
                return authenticate(request);
            }
        });
    }

    private SecurityIdentity authenticate(UsernamePasswordAuthenticationRequest request) {
        try (Session session = sessionFactory.openSession()) {
            session.setHibernateFlushMode(FlushMode.MANUAL);
            session.setDefaultReadOnly(true);
            return authenticate(session, request);
        } catch (SecurityException e) {
            log.debug("Authentication failed", e);
            throw new AuthenticationFailedException(e);
        }
    }

    protected <T> T getSingleUser(Query query) {
        @SuppressWarnings("unchecked")
        List<T> results = (List<T>) query.getResultList();
        return JpaIdentityProviderUtil.getSingleUser(results);
    }

    protected boolean requireActiveCDIRequestContext() {
        return false;
    }

    public abstract SecurityIdentity authenticate(EntityManager em,
            UsernamePasswordAuthenticationRequest request);

}
