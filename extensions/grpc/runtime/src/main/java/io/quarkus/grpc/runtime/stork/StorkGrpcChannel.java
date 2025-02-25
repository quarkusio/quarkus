package io.quarkus.grpc.runtime.stork;

import static io.quarkus.grpc.runtime.stork.StorkMeasuringCollector.STORK_MEASURE_TIME;
import static io.quarkus.grpc.runtime.stork.StorkMeasuringCollector.STORK_SERVICE_INSTANCE;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.Deadline;
import io.grpc.MethodDescriptor;
import io.grpc.internal.DelayedClientCall;
import io.quarkus.grpc.runtime.config.GrpcClientConfiguration;
import io.smallrye.mutiny.Uni;
import io.smallrye.stork.Stork;
import io.smallrye.stork.api.Service;
import io.smallrye.stork.api.ServiceInstance;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.client.GrpcClientChannel;

public class StorkGrpcChannel extends Channel implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(StorkGrpcChannel.class);

    private final Map<Long, ServiceInstance> services = new ConcurrentHashMap<>();
    private final Map<Long, Channel> channels = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;

    private final GrpcClient client;
    private final String serviceName;
    private final GrpcClientConfiguration.StorkConfig stork;
    private final Executor executor;

    private static class Context {
        Service service;
        boolean measureTime;
        ServiceInstance instance;
        InetSocketAddress address;
        Channel channel;
        AtomicReference<ServiceInstance> ref;
    }

    public StorkGrpcChannel(GrpcClient client, String serviceName, GrpcClientConfiguration.StorkConfig stork,
            Executor executor) {
        this.client = client;
        this.serviceName = serviceName;
        this.stork = stork;
        this.executor = executor;
        this.scheduler = new ScheduledThreadPoolExecutor(stork.threads());
        this.scheduler.scheduleAtFixedRate(this::refresh, stork.delay(), stork.period(), TimeUnit.SECONDS);
    }

    @Override
    public <RequestT, ResponseT> ClientCall<RequestT, ResponseT> newCall(MethodDescriptor<RequestT, ResponseT> methodDescriptor,
            CallOptions callOptions) {
        Service service = Stork.getInstance().getService(serviceName);
        if (service == null) {
            throw new IllegalStateException("No service definition for serviceName " + serviceName + " found.");
        }

        Context context = new Context();
        context.service = service;
        // handle this calls here
        Boolean measureTime = STORK_MEASURE_TIME.get();
        context.measureTime = measureTime != null && measureTime;
        context.ref = STORK_SERVICE_INSTANCE.get();

        DelayedClientCall<RequestT, ResponseT> delayed = new StorkDelayedClientCall<>(executor, scheduler,
                Deadline.after(stork.deadline(), TimeUnit.MILLISECONDS));

        asyncCall(methodDescriptor, callOptions, context)
                .onFailure()
                .retry()
                .atMost(stork.retries())
                .subscribe()
                .asCompletionStage()
                .thenApply(delayed::setCall)
                .thenAccept(Runnable::run)
                .exceptionally(t -> {
                    delayed.cancel("Failed to create new Stork ClientCall", t);
                    return null;
                });

        return delayed;
    }

    private <RequestT, ResponseT> Uni<ClientCall<RequestT, ResponseT>> asyncCall(
            MethodDescriptor<RequestT, ResponseT> methodDescriptor, CallOptions callOptions, Context context) {
        Uni<Context> entry = pickServiceInstanceWithChannel(context);
        return entry.map(c -> {
            ServiceInstance instance = c.instance;
            long serviceId = instance.getId();
            Channel channel = c.channel;
            try {
                services.put(serviceId, instance);
                channels.put(serviceId, channel);
                return channel.newCall(methodDescriptor, callOptions);
            } catch (Exception ex) {
                // remove, no good
                services.remove(serviceId);
                channels.remove(serviceId);
                throw new IllegalStateException(ex);
            }
        });
    }

    @Override
    public String authority() {
        return null;
    }

    @Override
    public void close() {
        scheduler.shutdown();
    }

    @Override
    public String toString() {
        return super.toString() + String.format(" [%s]", serviceName);
    }

    private void refresh() {
        // any better way to know which are OK / bad?
        services.clear();
        channels.clear();
    }

    private Uni<Context> pickServiceInstanceWithChannel(Context context) {
        Uni<ServiceInstance> uni = pickServerInstance(context.service, context.measureTime);
        return uni
                .map(si -> {
                    context.instance = si;
                    if (si.gatherStatistics() && context.ref != null) {
                        context.ref.set(si);
                    }
                    return context;
                })
                .invoke(this::checkSocketAddress)
                .invoke(c -> {
                    ServiceInstance instance = context.instance;
                    InetSocketAddress isa = context.address;
                    context.channel = channels.computeIfAbsent(instance.getId(), id -> {
                        SocketAddress address = SocketAddress.inetSocketAddress(isa.getPort(), isa.getHostName());
                        return new GrpcClientChannel(client, address);
                    });
                });
    }

    private Uni<ServiceInstance> pickServerInstance(Service service, boolean measureTime) {
        return Uni.createFrom()
                .deferred(() -> {
                    if (services.isEmpty()) {
                        return service.getInstances()
                                .invoke(l -> l.forEach(s -> services.put(s.getId(), s)));
                    } else {
                        List<ServiceInstance> list = new ArrayList<>(services.values());
                        return Uni.createFrom().item(list);
                    }
                })
                .map(ArrayList::new) // make it mutable
                .invoke(list -> {
                    // list should not be empty + sort by id
                    list.sort(Comparator.comparing(ServiceInstance::getId));
                })
                .map(list -> service.selectInstanceAndRecordStart(list, measureTime));
    }

    private void checkSocketAddress(Context context) {
        ServiceInstance instance = context.instance;
        Set<InetSocketAddress> socketAddresses = new HashSet<>();
        try {
            for (InetAddress inetAddress : InetAddress.getAllByName(instance.getHost())) {
                socketAddresses.add(new InetSocketAddress(inetAddress, instance.getPort()));
            }
        } catch (UnknownHostException e) {
            log.warn("Ignoring wrong host: '{}' for service name '{}'", instance.getHost(), serviceName, e);
        }

        if (!socketAddresses.isEmpty()) {
            context.address = socketAddresses.iterator().next(); // pick first
        } else {
            long serviceId = instance.getId();
            services.remove(serviceId);
            channels.remove(serviceId);
            throw new IllegalStateException("Failed to determine working socket addresses for service-name: " + serviceName);
        }
    }

    private static class StorkDelayedClientCall<RequestT, ResponseT> extends DelayedClientCall<RequestT, ResponseT> {
        public StorkDelayedClientCall(Executor callExecutor, ScheduledExecutorService scheduler, @Nullable Deadline deadline) {
            super(callExecutor, scheduler, deadline);
        }
    }
}
