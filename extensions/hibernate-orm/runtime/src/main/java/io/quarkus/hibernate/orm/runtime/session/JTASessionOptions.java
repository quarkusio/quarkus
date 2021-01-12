package io.quarkus.hibernate.orm.runtime.session;

import org.hibernate.FlushMode;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionFactory;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;

public class JTASessionOptions {
    private static final String KEY = JTASessionOptions.class.getName();

    public static void init(SessionFactory sessionFactory) {
        SessionBuilder<?> options = sessionFactory.withOptions()
                .autoClose(true) // .owner() is deprecated as well, so it looks like we need to rely on deprecated code...
                .connectionHandlingMode(
                        PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_BEFORE_TRANSACTION_COMPLETION)
                .flushMode(FlushMode.ALWAYS);
        sessionFactory.getProperties().put(KEY, options);
    }

    public static SessionBuilder<?> get(SessionFactory sessionFactory) {
        return (SessionBuilder<?>) sessionFactory.getProperties().get(KEY);
    }
}
