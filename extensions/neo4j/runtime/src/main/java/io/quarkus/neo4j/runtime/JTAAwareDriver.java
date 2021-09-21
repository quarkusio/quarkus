package io.quarkus.neo4j.runtime;

import java.util.concurrent.CompletionStage;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

import org.neo4j.driver.AccessMode;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Metrics;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.async.AsyncSession;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.types.TypeSystem;
import org.neo4j.driver.util.Experimental;

import io.quarkus.arc.Arc;

final class JTAAwareDriver implements Driver {

    private static final Object SESSION_KEY = new Object();
    private final Driver delegate;

    JTAAwareDriver(Driver delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean isEncrypted() {
        return delegate.isEncrypted();
    }

    @Override
    public Session session() {

        return this.session(SessionConfig.defaultConfig());
    }

    @Override
    public Session session(SessionConfig sessionConfig) {

        var synchronizationRegistryHandle = Arc.container().instance(TransactionSynchronizationRegistry.class);
        if (synchronizationRegistryHandle.isAvailable()) {

            var synchronizationRegistry = synchronizationRegistryHandle.get();
            if (synchronizationRegistry.getTransactionStatus() == Status.STATUS_ACTIVE) {

                var session = (Session) synchronizationRegistry.getResource(SESSION_KEY);
                if (session != null) {
                    return session;
                }

                return createAndRegisterJTAAwareSession(synchronizationRegistry, sessionConfig);
            }
        }

        return this.delegate.session(sessionConfig);
    }

    Session createAndRegisterJTAAwareSession(TransactionSynchronizationRegistry synchronizationRegistry,
            SessionConfig sessionConfig) {

        var configBuilder = SessionConfig.builder();
        // We will use write mode by default here… We have no way to determine this from the transactional annotation
        // If this is unwanted, make sure you use @Transactional(NEVER) and create a config on you own.
        configBuilder = configBuilder.withDefaultAccessMode(AccessMode.WRITE)
                .withBookmarks(sessionConfig.bookmarks());
        configBuilder = sessionConfig.database().map(configBuilder::withDatabase).orElse(configBuilder);
        configBuilder = sessionConfig.fetchSize().map(configBuilder::withFetchSize).orElse(configBuilder);

        var session = new JTAAwareSession(this.delegate.session(configBuilder.build()));

        synchronizationRegistry.putResource(SESSION_KEY, session);
        synchronizationRegistry.registerInterposedSynchronization(new Synchronization() {
            @Override
            public void beforeCompletion() {
            }

            @Override
            public void afterCompletion(int status) {

                switch (status) {
                    case Status.STATUS_ROLLEDBACK:
                        session.rollbackAndClose();
                        break;
                    case Status.STATUS_COMMITTED:
                        session.commitAndClose();
                        break;
                    default:
                        throw new RuntimeException("Don't know how to deal with transaction status " + status);
                }
            }
        });
        return session;
    }

    @Override
    public RxSession rxSession() {
        return delegate.rxSession();
    }

    @Override
    public RxSession rxSession(SessionConfig sessionConfig) {
        return delegate.rxSession(sessionConfig);
    }

    @Override
    public AsyncSession asyncSession() {
        return delegate.asyncSession();
    }

    @Override
    public AsyncSession asyncSession(SessionConfig sessionConfig) {
        return delegate.asyncSession(sessionConfig);
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public CompletionStage<Void> closeAsync() {
        return delegate.closeAsync();
    }

    @Override
    @Experimental
    public Metrics metrics() {
        return delegate.metrics();
    }

    @Override
    @Experimental
    public boolean isMetricsEnabled() {
        return delegate.isMetricsEnabled();
    }

    @Override
    @Experimental
    public TypeSystem defaultTypeSystem() {
        return delegate.defaultTypeSystem();
    }

    @Override
    public void verifyConnectivity() {
        delegate.verifyConnectivity();
    }

    @Override
    public CompletionStage<Void> verifyConnectivityAsync() {
        return delegate.verifyConnectivityAsync();
    }

    @Override
    public boolean supportsMultiDb() {
        return delegate.supportsMultiDb();
    }

    @Override
    public CompletionStage<Boolean> supportsMultiDbAsync() {
        return delegate.supportsMultiDbAsync();
    }
}
