package io.quarkus.hibernate.orm.runtime.session;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionFactory;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;

/**
 * A delegate for opening a JTA-enabled Hibernate ORM session.
 * <p>
 * The main purpose of this class is to cache session options when possible;
 * if we didn't care about caching, we could just replace any call to
 */
public class JTASessionOpener {
    public static JTASessionOpener create(SessionFactory sessionFactory) {
        final CurrentTenantIdentifierResolver currentTenantIdentifierResolver = sessionFactory
                .unwrap(SessionFactoryImplementor.class).getCurrentTenantIdentifierResolver();
        if (currentTenantIdentifierResolver == null) {
            // No tenant ID resolver: we can cache the options.
            return new JTASessionOpener(sessionFactory, createOptions(sessionFactory));
        } else {
            // There is a tenant ID resolver: we cannot cache the options.
            return new JTASessionOpener(sessionFactory, null);
        }
    }

    private static SessionBuilder createOptions(SessionFactory sessionFactory) {
        return sessionFactory.withOptions()
                .autoClose(true) // .owner() is deprecated as well, so it looks like we need to rely on deprecated code...
                .connectionHandlingMode(
                        PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_BEFORE_TRANSACTION_COMPLETION)
                .flushMode(FlushMode.ALWAYS);
    }

    private final SessionFactory sessionFactory;
    private final SessionBuilder cachedOptions;

    public JTASessionOpener(SessionFactory sessionFactory, SessionBuilder cachedOptions) {
        this.sessionFactory = sessionFactory;
        this.cachedOptions = cachedOptions;
    }

    public Session openSession() {
        SessionBuilder options = cachedOptions != null ? cachedOptions : createOptions(sessionFactory);
        return options.openSession();
    }
}
