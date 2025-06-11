package io.quarkus.oidc.client.runtime;

import static io.quarkus.oidc.client.runtime.OidcClientRecorder.createOidcClient;
import static io.quarkus.oidc.client.runtime.OidcClientRecorder.createOidcClientUni;
import static io.quarkus.oidc.client.runtime.OidcClientRecorder.createStaticOidcClients;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClientException;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.client.Tokens;
import io.quarkus.oidc.common.runtime.OidcTlsSupport;
import io.quarkus.runtime.Shutdown;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

@Singleton
public final class OidcClientsImpl implements OidcClients, Closeable {

    private static final Logger LOG = Logger.getLogger(OidcClientsImpl.class);

    private final OidcClient defaultClient;
    private final Map<String, OidcClient> staticOidcClients;
    private final Vertx vertx;
    private final OidcTlsSupport tlsSupport;
    private volatile PeriodicTask tokenRefreshTask;

    @Inject
    OidcClientsImpl(OidcClientsConfig oidcClientsConfig, Vertx vertx, TlsConfigurationRegistry registry) {
        this(oidcClientsConfig, vertx, OidcClientsConfig.getDefaultClient(oidcClientsConfig), OidcTlsSupport.of(registry));
    }

    private OidcClientsImpl(OidcClientsConfig oidcClientsConfig, Vertx vertx, OidcClientConfig defaultClientConfig,
            OidcTlsSupport tlsSupport) {
        this(createOidcClient(defaultClientConfig, defaultClientConfig.id().get(), vertx, tlsSupport),
                createStaticOidcClients(oidcClientsConfig, vertx, tlsSupport, defaultClientConfig),
                vertx, tlsSupport, createTokenRefreshPeriodicTask(oidcClientsConfig, vertx));
    }

    private OidcClientsImpl(OidcClient defaultClient, Map<String, OidcClient> staticOidcClients,
            Vertx vertx, OidcTlsSupport tlsSupport, PeriodicTask tokenRefreshTask) {
        this.defaultClient = defaultClient;
        this.staticOidcClients = staticOidcClients;
        this.vertx = vertx;
        this.tlsSupport = tlsSupport;
        this.tokenRefreshTask = tokenRefreshTask;
    }

    @Override
    public OidcClient getClient() {
        return defaultClient;
    }

    @Override
    public OidcClient getClient(String id) {
        return staticOidcClients.get(id);
    }

    @Override
    public void close() throws IOException {
        defaultClient.close();
        for (OidcClient client : staticOidcClients.values()) {
            client.close();
        }
    }

    @Override
    public Uni<OidcClient> newClient(OidcClientConfig clientConfig) {
        if (clientConfig.id().isEmpty()) {
            throw new OidcClientException("'id' property must be set");
        }
        return createOidcClientUni(clientConfig, clientConfig.id().get(), vertx, tlsSupport);
    }

    @Singleton
    @Produces
    @Unremovable
    OidcClient createOidcClientBean() {
        return defaultClient;
    }

    @PreDestroy
    void destroy() {
        try {
            close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Shutdown
    void shutdown() {
        if (tokenRefreshTask != null) {
            tokenRefreshTask.cancel();
        }
    }

    void registerTokenRefresh(OidcClient oidcClient, Supplier<Uni<Tokens>> tokensSupplier) {
        if (ClientProxy.unwrap(oidcClient) instanceof OidcClientImpl oidcClientImpl
                && oidcClientImpl.getConfig().refreshInterval().isPresent()) {
            Duration refreshInterval = oidcClientImpl.getConfig().refreshInterval().get();
            CancellableTask refreshTask = new CancellableTask() {

                private final AtomicInteger numOfErrorsInRow = new AtomicInteger();

                @Override
                public void run() {
                    tokensSupplier.get().subscribe().with(new Consumer<Tokens>() {
                        @Override
                        public void accept(Tokens ignored) {
                            if (numOfErrorsInRow.get() != 0) {
                                numOfErrorsInRow.set(0);
                            }
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) {
                            if (numOfErrorsInRow.incrementAndGet() >= 10) {
                                LOG.warnf(
                                        "Cancelling periodic token refresh for OIDC client '%s' as %d previous attempts failed",
                                        oidcClientImpl.getConfig().id().get(), numOfErrorsInRow.get());
                                cancel();
                            }
                        }
                    });
                }
            };
            if (tokenRefreshTask == null) {
                synchronized (this) {
                    if (tokenRefreshTask == null) {
                        tokenRefreshTask = new PeriodicTask(vertx);
                    }
                }
            }
            tokenRefreshTask.add(refreshTask, refreshInterval);
        }
    }

    private static PeriodicTask createTokenRefreshPeriodicTask(OidcClientsConfig oidcClientsConfig, Vertx vertx) {
        for (OidcClientConfig config : oidcClientsConfig.namedClients().values()) {
            if (config.refreshInterval().isPresent()) {
                return new PeriodicTask(vertx);
            }
        }
        return null;
    }

    private static final class PeriodicTask {

        private final Vertx vertx;
        private final Set<Long> timerIds;

        private PeriodicTask(Vertx vertx) {
            this.vertx = vertx;
            this.timerIds = new CopyOnWriteArraySet<>();
        }

        private void add(CancellableTask task, Duration refreshInterval) {
            final long timerId = scheduleTask(task, refreshInterval);
            timerIds.add(timerId);
        }

        private void cancel() {
            for (long timerId : timerIds) {
                vertx.cancelTimer(timerId);
            }
        }

        private long scheduleTask(CancellableTask task, Duration refreshInterval) {
            final long timerId = vertx.setPeriodic(0L, refreshInterval.toMillis(), task);
            task.cancel = new Runnable() {
                @Override
                public void run() {
                    timerIds.remove(timerId);
                    vertx.cancelTimer(timerId);
                }
            };
            return timerId;
        }
    }

    private static abstract class CancellableTask implements Handler<Long> {

        private volatile Runnable cancel;

        protected abstract void run();

        protected final void cancel() {
            if (cancel != null) {
                cancel.run();
            }
        }

        @Override
        public final void handle(Long ignored) {
            run();
        }
    }
}
