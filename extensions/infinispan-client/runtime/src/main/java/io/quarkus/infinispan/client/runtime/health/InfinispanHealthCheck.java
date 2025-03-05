package io.quarkus.infinispan.client.runtime.health;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;
import org.infinispan.client.hotrod.RemoteCacheManager;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.infinispan.client.InfinispanClientName;
import io.quarkus.infinispan.client.runtime.InfinispanClientRuntimeConfig;
import io.quarkus.infinispan.client.runtime.InfinispanClientUtil;
import io.quarkus.infinispan.client.runtime.InfinispanClientsRuntimeConfig;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.tuples.Tuple2;

@Readiness
@ApplicationScoped
public class InfinispanHealthCheck implements HealthCheck {

    private final List<InfinispanClientCheck> checks = new ArrayList<>();

    @Inject
    public InfinispanHealthCheck(InfinispanClientsRuntimeConfig config) {
        configure(config);
    }

    public void configure(InfinispanClientsRuntimeConfig config) {
        Iterable<InstanceHandle<RemoteCacheManager>> handle = Arc.container()
                .select(RemoteCacheManager.class, Any.Literal.INSTANCE)
                .handles();

        if (config.defaultInfinispanClient() != null) {
            RemoteCacheManager client = getClient(handle, null);
            if (client != null) {
                checks.add(new InfinispanClientCheck(InfinispanClientUtil.DEFAULT_INFINISPAN_CLIENT_NAME, client,
                        config.defaultInfinispanClient()));
            }
        }

        config.namedInfinispanClients().entrySet().forEach(new Consumer<Map.Entry<String, InfinispanClientRuntimeConfig>>() {
            @Override
            public void accept(Map.Entry<String, InfinispanClientRuntimeConfig> namedInfinispanClientConfig) {
                RemoteCacheManager client = getClient(handle, namedInfinispanClientConfig.getKey());
                if (client != null) {
                    checks.add(new InfinispanClientCheck(namedInfinispanClientConfig.getKey(), client,
                            namedInfinispanClientConfig.getValue()));
                }
            }
        });
    }

    private class HealthInfo {
        String state;
        String servers;
        int cachesCount;

        public HealthInfo(String state, String servers, int cachesCount) {
            this.state = state;
            this.servers = servers;
            this.cachesCount = cachesCount;
        }
    }

    private class InfinispanClientCheck implements Supplier<Uni<Tuple2<String, HealthInfo>>> {
        final String name;
        final RemoteCacheManager remoteCacheManager;
        final InfinispanClientRuntimeConfig config;

        InfinispanClientCheck(String name, RemoteCacheManager remoteCacheManager, InfinispanClientRuntimeConfig config) {
            this.name = name;
            this.remoteCacheManager = remoteCacheManager;
            this.config = config;
        }

        public Uni<Tuple2<String, HealthInfo>> get() {
            return Uni.createFrom().item(new Supplier<HealthInfo>() {
                @Override
                public HealthInfo get() {
                    String servers = Arrays.toString(remoteCacheManager.getServers());
                    int cacheNamesCount = remoteCacheManager.getCacheNames().size();
                    return new HealthInfo("OK", servers, cacheNamesCount);
                }
            })
                    .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                    .onItemOrFailure().transform(toResult(name));
        }
    }

    private BiFunction<HealthInfo, Throwable, Tuple2<String, HealthInfo>> toResult(String name) {
        return new BiFunction<HealthInfo, Throwable, Tuple2<String, HealthInfo>>() {
            @Override
            public Tuple2<String, HealthInfo> apply(HealthInfo healthInfo, Throwable failure) {
                return Tuple2.of(name, failure == null ? healthInfo : new HealthInfo(failure.getMessage(), null, -1));
            }
        };
    }

    private RemoteCacheManager getClient(Iterable<InstanceHandle<RemoteCacheManager>> handle, String name) {
        for (InstanceHandle<RemoteCacheManager> client : handle) {
            String n = getInfinispanClientName(client.getBean());
            if (name == null && n == null) {
                return client.get();
            }
            if (name != null && name.equals(n)) {
                return client.get();
            }
        }
        return null;
    }

    private String getInfinispanClientName(Bean<?> bean) {
        for (Object qualifier : bean.getQualifiers()) {
            if (qualifier instanceof InfinispanClientName) {
                return ((InfinispanClientName) qualifier).value();
            }
        }
        return null;
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("Infinispan cluster connection health check").up();
        List<Uni<Tuple2<String, HealthInfo>>> unis = new ArrayList<>();
        for (InfinispanClientCheck clientCheck : checks) {
            unis.add(clientCheck.get());
        }

        if (unis.isEmpty()) {
            return builder.build();
        }

        return Uni.combine().all().unis(unis)
                .collectFailures() // We collect all failures to avoid partial responses.
                .combinedWith(new Function<List<?>, HealthCheckResponse>() {
                    @Override
                    public HealthCheckResponse apply(List<?> list) {
                        return InfinispanHealthCheck.this.combine(list, builder);
                    }
                }).await().indefinitely(); // All checks fail after a timeout, so it won't be forever.
    }

    @SuppressWarnings("unchecked")
    private HealthCheckResponse combine(List<?> results, HealthCheckResponseBuilder builder) {
        for (Object result : results) {
            Tuple2<String, HealthInfo> tuple = (Tuple2<String, HealthInfo>) result;
            if ("OK".equalsIgnoreCase(tuple.getItem2().state)) {
                builder.up().withData(tuple.getItem1() + ".servers", tuple.getItem2().servers)
                        .withData(tuple.getItem1() + ".caches-size", tuple.getItem2().cachesCount);
            } else {
                builder.down()
                        .withData(tuple.getItem1(), "reason: " + tuple.getItem2().state);
            }
        }
        return builder.build();
    }
}
