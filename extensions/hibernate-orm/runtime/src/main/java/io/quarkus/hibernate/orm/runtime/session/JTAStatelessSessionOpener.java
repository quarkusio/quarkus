package io.quarkus.hibernate.orm.runtime.session;

import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.StatelessSessionBuilder;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * A delegate for opening a JTA-enabled Hibernate ORM StatelessSession.
 * <p>
 * The main purpose of this class is to cache session options when possible; if we didn't care about caching, we could
 * just replace any call to
 */
public class JTAStatelessSessionOpener {
    public static JTAStatelessSessionOpener create(SessionFactory sessionFactory) {
        final CurrentTenantIdentifierResolver currentTenantIdentifierResolver = sessionFactory
                .unwrap(SessionFactoryImplementor.class).getCurrentTenantIdentifierResolver();
        if (currentTenantIdentifierResolver == null) {
            // No tenant ID resolver: we can cache the options.
            return new JTAStatelessSessionOpener(sessionFactory, createOptions(sessionFactory));
        } else {
            // There is a tenant ID resolver: we cannot cache the options.
            return new JTAStatelessSessionOpener(sessionFactory, null);
        }
    }

    private static StatelessSessionBuilder createOptions(SessionFactory sessionFactory) {
        // TODO: what options would we pass here?
        return sessionFactory.withStatelessOptions();
    }

    private final SessionFactory sessionFactory;
    private final StatelessSessionBuilder cachedOptions;

    public JTAStatelessSessionOpener(SessionFactory sessionFactory, StatelessSessionBuilder cachedOptions) {
        this.sessionFactory = sessionFactory;
        this.cachedOptions = cachedOptions;
    }

    public StatelessSession openSession() {
        StatelessSessionBuilder options = cachedOptions != null ? cachedOptions : createOptions(sessionFactory);
        return options.openStatelessSession();
    }
}
