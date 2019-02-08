package org.jboss.shamrock.vertx.runtime;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.file.FileSystemOptions;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.PfxOptions;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * Produces a configured Vert.x instance.
 * It also exposes the Vert.x event bus.
 */
@ApplicationScoped
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class VertxProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(VertxProducer.class);

    /**
     * Enables or disables the Vert.x cache.
     */
    @Inject
    @ConfigProperty(name = "vertx.enable-caching", defaultValue = "true")
    boolean caching;

    /**
     * Enables or disabled the Vert.x classpath resource resolver.
     */
    @Inject
    @ConfigProperty(name = "vertx.enable-classpath-resolver", defaultValue = "true")
    public boolean classpathResolving;

    /**
     * The number of event loops. 2 x the number of core by default.
     */
    @Inject
    @ConfigProperty(name = "vertx.event-loop-poolsize")
    Optional<Integer> eventLoopsPoolSize;

    /**
     * The maximum amount of time the event loop can be blocked in second.
     */
    @ConfigProperty(name = "vertx.max-event-loop-execute-time", defaultValue = "2")
    long maxEventLoopExecuteTime;

    /**
     * The amount of time in second before a warning is displayed if the event loop is blocked.
     */
    @ConfigProperty(name = "vertx.warning-exception-time", defaultValue = "2")
    long warningExceptionTime;

    /**
     * The size of the worker thread pool.
     */
    @ConfigProperty(name = "vertx.worker-pool-size", defaultValue = "20")
    int workerPoolSize;

    /**
     * The maximum amount of time, in second, the worker thread can be blocked.
     */
    @ConfigProperty(name = "vertx.max-worker-execute-time", defaultValue = "60")
    long maxWorkerExecuteTime;

    /**
     * The size of the internal thread pool (used for the file system).
     */
    @ConfigProperty(name = "vertx.internal-blocking-pool-size", defaultValue = "20")
    int internalBlockingPoolSize;

    /**
     * Enables the async DNS resolver.
     */
    @ConfigProperty(name = "vertx.use-async-dns", defaultValue = "false")
    boolean useAsyncDNS;

    /**
     * The host name to be used for clustering.
     */
    @ConfigProperty(name = "vertx.cluster.host", defaultValue = "localhost")
    String clusterHost;

    /**
     * The port to be used for clustering.
     */
    @ConfigProperty(name = "vertx.cluster.port")
    Optional<Integer> clusterPort;

    /**
     * The public facing host name to be used when clustering.
     */
    @ConfigProperty(name = "vertx.cluster.public-host")
    Optional<String> clusterPublicHost;

    /**
     * The public facing host name to be used when clustering.
     */
    @ConfigProperty(name = "vertx.cluster.public-port")
    Optional<Integer> clusterPublicPort;

    /**
     * Enables or disables the clustering.
     */
    @ConfigProperty(name = "vertx.cluster.enabled")
    boolean clustered;

    /**
     * The cluster ping interval, in seconds.
     */
    @ConfigProperty(name = "vertx.cluster.ping-interval", defaultValue = "2")
    long pingInterval;

    /**
     * The ping reply interval, in seconds.
     */
    @ConfigProperty(name = "vertx.cluster.ping-reply-interval", defaultValue = "2")
    long pingReplyInterval;

    /**
     * Path to the key file used for the SSL communication in the event bus.
     * The extension determines the type of key. Are supported {@code .pem}, {@code .jks},and {@code .pfx}.
     * In the case of {@code pem} keys, a comma-separated list can be given.
     */
    @ConfigProperty(name = "vertx.eventbus.key-store.path")
    Optional<String> keyStorePath;

    /**
     * The list of certificates for the SSL communication in the event bus.
     * This value is only used if {@code vertx.eventbus.key-store.path} references a {@code pem} file.
     * The expected value is a comma-separated list paths to the certificate files (Pem format).
     */
    @ConfigProperty(name = "vertx.eventbus.key-store.certs")
    Optional<String> keyStoreCerts;

    /**
     * The password used to open the key file.
     * This value is only used  if {@code vertx.eventbus.key-store.path} references a {@code jks} or a {@code pfx} file.
     */
    @ConfigProperty(name = "vertx.eventbus.key-store.password")
    Optional<String> keyStorePassword;

    /**
     * Path to the certificate file used for the SSL communication in the event bus.
     * The extension determines the type of key. Are supported {@code .pem}, {@code .jks},and {@code .pfx}.
     * In the case of {@code pem} keys, a comma-separated list can be given.
     */
    @ConfigProperty(name = "vertx.eventbus.trust-store.path")
    Optional<String> trustStorePath;

    /**
     * Password for the certificate file used for the SSL communication in the event bus.
     * This value is only used  if {@code vertx.eventbus.trust-store.path} references a {@code jks} or a {@code pfx} file.
     */
    @ConfigProperty(name = "vertx.eventbus.trust-store.password")
    Optional<String> trustStorePassword;

    /**
     * The client authentication used for the event bus communication.
     */
    @ConfigProperty(name = "vertx.eventbus.client-auth")
    Optional<String> clientAuth;

    /**
     * The connect timeout used for the event bus communication in seconds.
     */
    @ConfigProperty(name = "vertx.eventbus.connect-timeout")
    Optional<Long> connectTimeout;

    /**
     * The idle timeout in seconds for the event bus communication.
     */
    @ConfigProperty(name = "vertx.eventbus.idle-timeout")
    Optional<Long> idleTimeout;

    /**
     * The number of reconnection attempts for the event bus communication.
     */
    @ConfigProperty(name = "vertx.eventbus.reconnect-attempts")
    Optional<Integer> reconnectAttempts;

    /**
     * The reconnection interval in seconds for the event bus communication.
     */
    @ConfigProperty(name = "vertx.eventbus.reconnect-interval")
    Optional<Long> reconnectInterval;

    /**
     * Whether or not to reuse the address for the event bus communication.
     */
    @ConfigProperty(name = "vertx.eventbus.reuse-address")
    Optional<Boolean> reuseAddress;

    /**
     * Whether or not to reuse the port for the event bus communication.
     */
    @ConfigProperty(name = "vertx.eventbus.reuse-port")
    Optional<Boolean> reusePort;

    /**
     * Enables or Disabled SSL for the event bus.
     */
    @ConfigProperty(name = "vertx.eventbus.ssl", defaultValue = "false")
    boolean ssl;

    /**
     * Enables or disables the trust-all parameter for the event bus.
     */
    @ConfigProperty(name = "vertx.eventbus.trust-all", defaultValue = "false")
    public boolean trustAll;

    /**
     * The vert.x instance.
     */
    private volatile Vertx vertx;

    private void initialize() {
        VertxOptions options = computeVertxOptions();

        if (!useAsyncDNS) {
            System.setProperty("vertx.disableDnsResolver", "true");
        }

        System.setProperty("vertx.cacheDirBase", System.getProperty("java.io.tmpdir"));

        if (options.isClustered()) {
            AtomicReference<Throwable> failure = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            Vertx.clusteredVertx(options, ar -> {
                if (ar.failed()) {
                    failure.set(ar.cause());
                } else {
                    this.vertx = ar.result();
                }
                latch.countDown();
            });
            try {
                latch.await();
                if (failure.get() != null) {
                    throw new IllegalStateException("Unable to initialize the Vert.x instance", failure.get());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Unable to initialize the Vert.x instance", e);
            }
        } else {
            this.vertx = Vertx.vertx(options);
        }
    }

    private VertxOptions computeVertxOptions() {
        VertxOptions options = new VertxOptions();
        // Order matters, as the cluster options modifies the event bus options.
        setEventBusOptions(options);
        initializeClusterOptions(options);

        options.setFileSystemOptions(new FileSystemOptions()
                .setFileCachingEnabled(caching)
                .setClassPathResolvingEnabled(classpathResolving));
        options.setWorkerPoolSize(workerPoolSize);
        options.setBlockedThreadCheckInterval(TimeUnit.SECONDS.toMillis(warningExceptionTime));
        options.setInternalBlockingPoolSize(internalBlockingPoolSize);

        eventLoopsPoolSize.ifPresent(options::setEventLoopPoolSize);

        options.setMaxEventLoopExecuteTime(TimeUnit.SECONDS.toNanos(maxEventLoopExecuteTime))
                .setMaxWorkerExecuteTime(TimeUnit.SECONDS.toNanos(maxWorkerExecuteTime));

        options.setWarningExceptionTime(TimeUnit.SECONDS.toNanos(warningExceptionTime));

        return options;
    }

    @PreDestroy
    public void destroy() {
        if (vertx != null) {
            CountDownLatch latch = new CountDownLatch(1);
            vertx.close(ar -> {
                if (ar.failed()) {
                    LOGGER.error("Failure caught while closing the Vert.x instance", ar.cause());
                }
                latch.countDown();
            });
            try {
                if (!latch.await(1, TimeUnit.MINUTES)) {
                    LOGGER.error("Timeout while waiting for the Vert.x instance to be closed");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.error("Interrupted while waiting for the Vert.x instance to be closed");
            }
        }
    }

    private void initializeClusterOptions(VertxOptions options) {
        if (clustered) {
            options.setClustered(true);
            options.setClusterPingInterval(TimeUnit.SECONDS.toMillis(pingInterval));
            options.setClusterPingReplyInterval(TimeUnit.SECONDS.toMillis(pingReplyInterval));
            options.setClusterHost(clusterHost);
            clusterPort.ifPresent(options::setClusterPort);
            clusterPublicHost.ifPresent(options::setClusterPublicHost);
            clusterPublicPort.ifPresent(options::setClusterPublicPort);
        }
    }

    private void setEventBusOptions(VertxOptions options) {
        if (! clustered) {
            return;
        }

        EventBusOptions opts = new EventBusOptions();
        clientAuth.ifPresent(v -> opts.setClientAuth(ClientAuth.valueOf(v.toUpperCase())));
        connectTimeout.ifPresent(v -> opts.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(v)));
        idleTimeout.ifPresent(v -> opts.setIdleTimeout((int) TimeUnit.SECONDS.toMillis(v)));
        opts.setSsl(ssl);
        reconnectAttempts.ifPresent(opts::setReconnectAttempts);
        reconnectInterval.ifPresent(v -> opts.setReconnectInterval(TimeUnit.SECONDS.toMillis(v)));
        reuseAddress.ifPresent(opts::setReuseAddress);
        reusePort.ifPresent(opts::setReusePort);
        opts.setTrustAll(trustAll);

        if (keyStorePath.isPresent()) {
            String path = keyStorePath.get();
            if (path.contains(",") || path.trim().endsWith(".pem")) {
                // PEM - can be lists
                PemKeyCertOptions o = new PemKeyCertOptions();
                Pattern.compile(",").splitAsStream(path).map(String::trim).forEach(o::addKeyPath);
                Pattern.compile(",").splitAsStream(keyStoreCerts.orElse("")).map(String::trim).forEach(o::addCertPath);
                opts.setPemKeyCertOptions(o);
            } else if (path.trim().endsWith(".jks")) {
                // JKS
                JksOptions o = new JksOptions();
                o.setPath(path);
                keyStorePassword.ifPresent(o::setPassword);
                opts.setKeyStoreOptions(o);
            } else if (path.trim().endsWith(".pfx")) {
                // PFX
                PfxOptions o = new PfxOptions();
                o.setPath(path);
                keyStorePassword.ifPresent(o::setPassword);
                opts.setPfxKeyCertOptions(o);
            }
        }

        if (trustStorePath.isPresent()) {
            String path = trustStorePath.get();
            if (path.contains(",") || path.trim().endsWith(".pem")) {
                // PEM - can be lists
                PemTrustOptions o = new PemTrustOptions();
                Pattern.compile(",").splitAsStream(keyStoreCerts.orElse("")).map(String::trim).forEach(o::addCertPath);
                opts.setPemTrustOptions(o);
            } else if (path.trim().endsWith(".jks")) {
                // JKS
                JksOptions o = new JksOptions();
                o.setPath(path);
                keyStorePassword.ifPresent(o::setPassword);
                opts.setTrustStoreOptions(o);
            } else if (path.trim().endsWith(".pfx")) {
                // PFX
                PfxOptions o = new PfxOptions();
                o.setPath(path);
                keyStorePassword.ifPresent(o::setPassword);
                opts.setPfxTrustOptions(o);
            }
        }

        options.setEventBusOptions(opts);
    }

    @Singleton
    @Produces
    public synchronized Vertx vertx() {
        if (vertx != null) {
            return vertx;
        }
        initialize();
        return this.vertx;
    }

    @Singleton
    @Produces
    public synchronized EventBus eventbus() {
        if (vertx == null) {
            initialize();
        }
        return this.vertx.eventBus();
    }

    void registerMessageConsumers(List<Map<String, String>> messageConsumers) {
        if (!messageConsumers.isEmpty()) {
            EventBus eventBus = eventbus();
            CountDownLatch latch = new CountDownLatch(messageConsumers.size());
            for (Map<String, String> messageConsumer : messageConsumers) {
                EventConsumerInvoker invoker = createInvoker(messageConsumer.get("invokerClazz"));
                String address = messageConsumer.get("address");
                MessageConsumer<Object> consumer;
                if (Boolean.valueOf(messageConsumer.get("local"))) {
                    consumer = eventBus.localConsumer(address);
                } else {
                    consumer = eventBus.consumer(address);
                }
                consumer.handler(m -> invoker.invoke(m));
                consumer.completionHandler(ar -> {
                    if (ar.succeeded()) {
                        latch.countDown();
                    }
                });
            }
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Unable to register all message consumer methods", e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private EventConsumerInvoker createInvoker(String invokerClassName) {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = VertxProducer.class.getClassLoader();
            }
            Class<? extends EventConsumerInvoker> invokerClazz = (Class<? extends EventConsumerInvoker>) cl.loadClass(invokerClassName);
            return invokerClazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalStateException("Unable to create invoker: " + invokerClassName, e);
        }
    }
}
