package io.quarkus.vertx.runtime;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Template;
import io.quarkus.vertx.ConsumeEvent;
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

@Template
public class VertxTemplate {

    static volatile Vertx vertx;
    static volatile List<MessageConsumer<?>> messageConsumers;

    public RuntimeValue<Vertx> configureVertx(BeanContainer container, VertxConfiguration config,
            Map<String, ConsumeEvent> messageConsumerConfigurations,
            LaunchMode launchMode, ShutdownContext shutdown) {

        initialize(config);
        registerMessageConsumers(messageConsumerConfigurations);

        VertxProducer producer = container.instance(VertxProducer.class);
        producer.initialize(vertx);
        if (launchMode == LaunchMode.DEVELOPMENT) {
            shutdown.addShutdownTask(new Runnable() {
                @Override
                public void run() {
                    unregisterMessageConsumers();
                }
            });
        } else {
            shutdown.addShutdownTask(new Runnable() {
                @Override
                public void run() {
                    destroy();
                }
            });
        }
        return new RuntimeValue<Vertx>(vertx);
    }

    void initialize(VertxConfiguration conf) {
        if (vertx != null) {
            return;
        }
        if (conf == null) {
            vertx = Vertx.vertx();
            return;
        }

        VertxOptions options = convertToVertxOptions(conf);

        if (!conf.useAsyncDNS) {
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
                    vertx = ar.result();
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
            vertx = Vertx.vertx(options);
        }

        messageConsumers = new ArrayList<>();
    }

    private VertxOptions convertToVertxOptions(VertxConfiguration conf) {
        VertxOptions options = new VertxOptions();
        // Order matters, as the cluster options modifies the event bus options.
        setEventBusOptions(conf, options);
        initializeClusterOptions(conf, options);

        options.setFileSystemOptions(new FileSystemOptions()
                .setFileCachingEnabled(conf.caching)
                .setClassPathResolvingEnabled(conf.classpathResolving));
        options.setWorkerPoolSize(conf.workerPoolSize);
        options.setBlockedThreadCheckInterval(conf.warningExceptionTime.toMillis());
        options.setInternalBlockingPoolSize(conf.internalBlockingPoolSize);
        if (conf.eventLoopsPoolSize.isPresent()) {
            options.setEventLoopPoolSize(conf.eventLoopsPoolSize.getAsInt());
        }
        // TODO - Add the ability to configure these times in ns when long will be supported
        //  options.setMaxEventLoopExecuteTime(conf.maxEventLoopExecuteTime)
        //         .setMaxWorkerExecuteTime(conf.maxWorkerExecuteTime)
        options.setWarningExceptionTime(conf.warningExceptionTime.toNanos());

        return options;
    }

    void destroy() {
        if (vertx != null) {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Throwable> problem = new AtomicReference<>();
            vertx.close(ar -> {
                if (ar.failed()) {
                    problem.set(ar.cause());
                }
                latch.countDown();
            });
            try {
                latch.await();
                if (problem.get() != null) {
                    throw new IllegalStateException("Error when closing Vertx instance", problem.get());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted when closing Vertx instance", e);
            }
            vertx = null;
            messageConsumers = null;
        }
    }

    private void initializeClusterOptions(VertxConfiguration conf, VertxOptions options) {
        ClusterConfiguration cluster = conf.cluster;
        options.setClustered(cluster.clustered);
        options.setClusterPingReplyInterval(cluster.pingReplyInterval.toMillis());
        options.setClusterPingInterval(cluster.pingInterval.toMillis());
        if (cluster.host != null) {
            options.setClusterHost(cluster.host);
        }
        if (cluster.port.isPresent()) {
            options.setClusterPort(cluster.port.getAsInt());
        }
        cluster.publicHost.ifPresent(options::setClusterPublicHost);
        if (cluster.publicPort.isPresent()) {
            options.setClusterPort(cluster.publicPort.getAsInt());
        }
    }

    private void setEventBusOptions(VertxConfiguration conf, VertxOptions options) {
        EventBusConfiguration eb = conf.eventbus;
        EventBusOptions opts = new EventBusOptions();
        opts.setAcceptBacklog(eb.acceptBacklog.orElse(-1));
        opts.setClientAuth(ClientAuth.valueOf(eb.clientAuth.toUpperCase()));
        opts.setConnectTimeout((int) (Math.min(Integer.MAX_VALUE, eb.connectTimeout.toMillis())));
        // todo: use timeUnit cleverly
        opts.setIdleTimeout(
                eb.idleTimeout.isPresent() ? (int) Math.max(1, Math.min(Integer.MAX_VALUE, eb.idleTimeout.get().getSeconds()))
                        : 0);
        opts.setSendBufferSize(eb.sendBufferSize.orElse(-1));
        opts.setSoLinger(eb.soLinger.orElse(-1));
        opts.setSsl(eb.ssl);
        opts.setReceiveBufferSize(eb.receiveBufferSize.orElse(-1));
        opts.setReconnectAttempts(eb.reconnectAttempts);
        opts.setReconnectInterval(eb.reconnectInterval.toMillis());
        opts.setReuseAddress(eb.reuseAddress);
        opts.setReusePort(eb.reusePort);
        opts.setTrafficClass(eb.trafficClass.orElse(-1));
        opts.setTcpKeepAlive(eb.tcpKeepAlive);
        opts.setTcpNoDelay(eb.tcpNoDelay);
        opts.setTrustAll(eb.trustAll);

        // Certificates and trust.
        if (eb.keyCertificatePem != null) {
            List<String> certs = new ArrayList<>();
            List<String> keys = new ArrayList<>();
            eb.keyCertificatePem.certs.ifPresent(
                    s -> certs.addAll(Pattern.compile(",").splitAsStream(s).map(String::trim).collect(Collectors.toList())));
            eb.keyCertificatePem.keys.ifPresent(
                    s -> keys.addAll(Pattern.compile(",").splitAsStream(s).map(String::trim).collect(Collectors.toList())));
            PemKeyCertOptions o = new PemKeyCertOptions()
                    .setCertPaths(certs)
                    .setKeyPaths(keys);
            opts.setPemKeyCertOptions(o);
        }

        if (eb.keyCertificateJks != null) {
            JksOptions o = new JksOptions();
            eb.keyCertificateJks.path.ifPresent(o::setPath);
            eb.keyCertificateJks.password.ifPresent(o::setPassword);
            opts.setKeyStoreOptions(o);
        }

        if (eb.keyCertificatePfx != null) {
            PfxOptions o = new PfxOptions();
            eb.keyCertificatePfx.path.ifPresent(o::setPath);
            eb.keyCertificatePfx.password.ifPresent(o::setPassword);
            opts.setPfxKeyCertOptions(o);
        }

        if (eb.trustCertificatePem != null) {
            eb.trustCertificatePem.certs.ifPresent(s -> {
                PemTrustOptions o = new PemTrustOptions();
                Pattern.compile(",").splitAsStream(s).map(String::trim).forEach(o::addCertPath);
                opts.setPemTrustOptions(o);
            });
        }

        if (eb.trustCertificateJks != null) {
            JksOptions o = new JksOptions();
            eb.trustCertificateJks.path.ifPresent(o::setPath);
            eb.trustCertificateJks.password.ifPresent(o::setPassword);
            opts.setTrustStoreOptions(o);
        }

        if (eb.trustCertificatePfx != null) {
            PfxOptions o = new PfxOptions();
            eb.trustCertificatePfx.path.ifPresent(o::setPath);
            eb.trustCertificatePfx.password.ifPresent(o::setPassword);
            opts.setPfxTrustOptions(o);
        }
        options.setEventBusOptions(opts);
    }

    void registerMessageConsumers(Map<String, ConsumeEvent> messageConsumerConfigurations) {
        if (!messageConsumerConfigurations.isEmpty()) {
            EventBus eventBus = vertx.eventBus();
            CountDownLatch latch = new CountDownLatch(messageConsumerConfigurations.size());
            for (Entry<String, ConsumeEvent> entry : messageConsumerConfigurations.entrySet()) {
                EventConsumerInvoker invoker = createInvoker(entry.getKey());
                String address = entry.getValue().value();
                MessageConsumer<Object> consumer;
                if (entry.getValue().local()) {
                    consumer = eventBus.localConsumer(address);
                } else {
                    consumer = eventBus.consumer(address);
                }
                consumer.handler(m -> {
                    try {
                        invoker.invoke(m);
                    } catch (Throwable e) {
                        m.fail(ConsumeEvent.FAILURE_CODE, e.getMessage());
                    }
                });
                consumer.completionHandler(ar -> {
                    if (ar.succeeded()) {
                        latch.countDown();
                    }
                });
                messageConsumers.add(consumer);
            }
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Unable to register all message consumer methods", e);
            }
        }
    }

    void unregisterMessageConsumers() {
        CountDownLatch latch = new CountDownLatch(messageConsumers.size());
        for (MessageConsumer<?> messageConsumer : messageConsumers) {
            messageConsumer.unregister(ar -> {
                if (ar.succeeded()) {
                    latch.countDown();
                }
            });
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Unable to unregister all message consumer methods", e);
        }
        messageConsumers.clear();
    }

    @SuppressWarnings("unchecked")
    private EventConsumerInvoker createInvoker(String invokerClassName) {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = VertxProducer.class.getClassLoader();
            }
            Class<? extends EventConsumerInvoker> invokerClazz = (Class<? extends EventConsumerInvoker>) cl
                    .loadClass(invokerClassName);
            return invokerClazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException
                | InvocationTargetException e) {
            throw new IllegalStateException("Unable to create invoker: " + invokerClassName, e);
        }
    }

}
