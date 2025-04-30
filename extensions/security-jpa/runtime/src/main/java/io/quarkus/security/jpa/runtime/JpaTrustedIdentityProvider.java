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
import io.quarkus.security.identity.request.TrustedAuthenticationRequest;
import io.quarkus.security.jpa.common.runtime.JpaIdentityProviderUtil;
import io.smallrye.mutiny.Uni;

public abstract class JpaTrustedIdentityProvider implements IdentityProvider<TrustedAuthenticationRequest> {

    private static Logger log = Logger.getLogger(JpaTrustedIdentityProvider.class);

    @Inject
    SessionFactory sessionFactory;

    @Override
    public Class<TrustedAuthenticationRequest> getRequestType() {
        return TrustedAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(TrustedAuthenticationRequest request,
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

    private SecurityIdentity authenticate(TrustedAuthenticationRequest request) {
        try (Session session = sessionFactory.openSession()) {
            session.setHibernateFlushMode(FlushMode.MANUAL);
            session.setDefaultReadOnly(true);
            return authenticate(session, request);
        } catch (SecurityException e) {
            log.debug("Authentication failed", e);
            throw new AuthenticationFailedException(e);
        }
    }

    protected boolean requireActiveCDIRequestContext() {
        return false;
    }

    protected <T> T getSingleUser(Query query) {
        @SuppressWarnings("unchecked")
        List<T> results = (List<T>) query.getResultList();
        return JpaIdentityProviderUtil.getSingleUser(results);
    }

    public abstract SecurityIdentity authenticate(EntityManager em,
            TrustedAuthenticationRequest request);
}
