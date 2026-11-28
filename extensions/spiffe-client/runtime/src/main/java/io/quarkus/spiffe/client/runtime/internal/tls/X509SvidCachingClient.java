package io.quarkus.spiffe.client.runtime.internal.tls;

import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.spiffe.client.SpiffeAuthorizationException;
import io.quarkus.spiffe.client.SpiffeConnectionException;
import io.quarkus.spiffe.svid.x509.WorkloadCertificateDocument;
import io.quarkus.spiffe.svid.x509.X509SvidClient;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.grpc.client.InvalidStatusException;
import io.vertx.grpc.common.GrpcStatus;

interface X509SvidCachingClient {

    WorkloadCertificateDocument getCurrentWorkloadCertificate();

    final class X509SvidCachingClientImpl implements X509SvidCachingClient {

        private static final Logger LOG = Logger.getLogger(X509SvidCachingClient.class);

        private final X509SvidClient delegate;
        private final Timing timing;
        private final AtomicBoolean fetching = new AtomicBoolean();
        private volatile Duration refreshPeriod;
        private volatile Current current;
        private volatile boolean closed;
        private volatile boolean failing; // the first failure is a warning, the recovery an info, the rest debug
        private volatile Duration warnedLifetime;

        X509SvidCachingClientImpl(X509SvidClient delegate) {
            this(delegate, Timing.DEFAULT);
        }

        X509SvidCachingClientImpl(X509SvidClient delegate, Timing timing) {
            this.delegate = delegate;
            this.timing = timing;
        }

        // completes once the first document is in place, fails when none was obtained within the startup timeout
        void initialize(Vertx vertx, Duration refreshPeriod, Consumer<Runnable> onStop, Consumer<Throwable> onFailure,
                Consumer<WorkloadCertificateDocument> onComplete) {
            if (refreshPeriod.isNegative() || refreshPeriod.isZero()) {
                throw new ConfigurationException("The X.509-SVID refresh period must be positive, got " + refreshPeriod);
            }
            this.refreshPeriod = refreshPeriod;
            fetch()
                    .onFailure(X509SvidCachingClientImpl::retryable).retry()
                    .withBackOff(timing.startupBackoff(), timing.startupMaxBackoff())
                    .expireIn(timing.startupTimeout().toMillis())
                    .onFailure().transform(failure -> new SpiffeConnectionException(retryable(failure)
                            ? "No X.509-SVID was obtained from the Workload API within " + timing.startupTimeout()
                            : "The Workload API at 'quarkus.spiffe-client.endpoint-socket' rejected the X.509-SVID request,"
                                    + " retrying cannot fix it",
                            failure))
                    .invoke(() -> {
                        long timer = vertx.setPeriodic(refreshPeriod.toMillis(), ignored -> refresh());
                        onStop.accept(() -> {
                            vertx.cancelTimer(timer);
                            close();
                        });
                    })
                    .subscribe()
                    .with(onComplete, onFailure);
        }

        @Override
        public WorkloadCertificateDocument getCurrentWorkloadCertificate() {
            Current current = this.current;
            if (current == null) {
                return null;
            }
            if (Instant.now().isAfter(current.notAfter)) {
                if (current.expiryWarned.compareAndSet(false, true)) {
                    LOG.warnf("The X.509-SVID %s expired at %s and no replacement was fetched; the TLS configurations present"
                            + " no certificate and trust no peer until one is", current.document.subject(), current.notAfter);
                }
                return null;
            }
            return current.document;
        }

        void close() {
            closed = true;
            current = null;
        }

        private void refresh() {
            // the previous fetch may still be running, bounded by the timeouts of the client; this tick is skipped then
            if (closed || !fetching.compareAndSet(false, true)) {
                return;
            }
            Uni<WorkloadCertificateDocument> fetch = fetch();
            if (refreshPeriod.compareTo(timing.tickRetryMinPeriod()) >= 0) {
                fetch = fetch.onFailure(failure -> !closed && retryable(failure)).retry()
                        .withBackOff(timing.tickRetryDelay(), timing.tickRetryDelay()).atMost(1);
            }
            fetch.subscribe().with(document -> fetching.set(false), failure -> fetching.set(false));
        }

        private Uni<WorkloadCertificateDocument> fetch() {
            return delegate.getWorkloadCertificate().onItem().invoke(this::accept).onFailure().invoke(this::failed);
        }

        private void accept(WorkloadCertificateDocument document) {
            if (closed) {
                return;
            }
            if (failing) {
                failing = false;
                LOG.infof("Fetching the X.509-SVID from the Workload API works again");
            }
            Current previous = current;
            if (previous != null && previous.sameContent(document)) {
                LOG.tracef("The X.509-SVID %s is unchanged", document.subject());
                return;
            }
            X509Certificate leaf = document.certificateChain().chain().getFirst();
            current = new Current(document, leaf.getNotAfter().toInstant());
            LOG.debugf("The X.509-SVID %s is valid until %s", document.subject(), leaf.getNotAfter());
            warnAboutRefreshPeriod(leaf);
        }

        private void failed(Throwable failure) {
            if (closed) {
                return;
            }
            if (failure instanceof SpiffeAuthorizationException && current != null) {
                // Workload API 5.2.1: a workload the agent no longer authorizes should stop using its SVID
                current = null;
                LOG.warnf("The Workload API no longer issues an X.509-SVID to this workload (%s); the TLS configurations"
                        + " present no certificate and trust no peer until it does", failure.getMessage());
            }
            if (!failing) {
                failing = true;
                LOG.warnf(failure, "Fetching the X.509-SVID from the Workload API failed");
            } else {
                LOG.debugf(failure, "Fetching the X.509-SVID from the Workload API keeps failing");
            }
        }

        // SPIFFE Workload Endpoint 6: INVALID_ARGUMENT is a client error that retrying cannot fix, UNIMPLEMENTED means the
        // endpoint has no FetchX509SVID
        private static boolean retryable(Throwable failure) {
            return !(failure.getCause() instanceof InvalidStatusException e
                    && (e.actualStatus() == GrpcStatus.INVALID_ARGUMENT || e.actualStatus() == GrpcStatus.UNIMPLEMENTED));
        }

        private void warnAboutRefreshPeriod(X509Certificate leaf) {
            // the agent rotates at about half of the lifetime, a quarter leaves at least one fetch after the latest rotation
            Duration lifetime = Duration.between(leaf.getNotBefore().toInstant(), leaf.getNotAfter().toInstant());
            if (refreshPeriod.compareTo(lifetime.dividedBy(4)) > 0 && !lifetime.equals(warnedLifetime)) {
                warnedLifetime = lifetime;
                LOG.warnf("The X.509-SVID refresh period %s exceeds a quarter of the SVID lifetime %s, a rotated SVID may not"
                        + " be fetched before the current one expires; lower 'quarkus.spiffe-client.x509-svid.refresh-period'",
                        refreshPeriod, lifetime);
            }
        }

        record Timing(Duration startupTimeout, Duration startupBackoff, Duration startupMaxBackoff, Duration tickRetryDelay,
                Duration tickRetryMinPeriod) {

            // a failed tick is retried once when the next tick is far away, with a short period the next tick is the retry
            static final Timing DEFAULT = new Timing(Duration.ofMinutes(1), Duration.ofSeconds(1), Duration.ofSeconds(30),
                    Duration.ofSeconds(10), Duration.ofMinutes(1));
        }

        private record Current(WorkloadCertificateDocument document, Instant notAfter, AtomicBoolean expiryWarned) {

            Current(WorkloadCertificateDocument document, Instant notAfter) {
                this(document, notAfter, new AtomicBoolean());
            }

            boolean sameContent(WorkloadCertificateDocument other) {
                return document.certificateChain().chain().equals(other.certificateChain().chain())
                        && document.trustBundle().certificates().equals(other.trustBundle().certificates());
            }
        }
    }

}
