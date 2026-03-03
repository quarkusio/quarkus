package io.quarkus.mongodb.health;

import static io.quarkus.mongodb.runtime.MongoClientBeanUtil.mongoClientName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;

import org.bson.Document;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import com.mongodb.client.MongoClient;

import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.runtime.MongoClientConfig;
import io.quarkus.mongodb.runtime.MongoConfig;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.tuples.Tuple2;

@Readiness
@ApplicationScoped
public class MongoHealthCheck implements HealthCheck {
    /**
     * @deprecated Use {@link MongoConfig#DEFAULT_CLIENT_NAME} instead.
     */
    @Deprecated(forRemoval = true, since = "3.33")
    public static final String CLIENT_DEFAULT = MongoConfig.DEFAULT_CLIENT_NAME;
    /**
     * @deprecated Use {@link MongoConfig#DEFAULT_REACTIVE_CLIENT_NAME} instead.
     */
    @Deprecated(forRemoval = true, since = "3.33")
    public static final String CLIENT_DEFAULT_REACTIVE = MongoConfig.DEFAULT_REACTIVE_CLIENT_NAME;

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final Document COMMAND = new Document("ping", 1);

    private final List<Supplier<Uni<Tuple2<String, String>>>> checks = new ArrayList<>();

    @Inject
    MongoConfig mongoConfig;
    @Any
    @Inject
    InjectableInstance<MongoClient> mongoClient;
    @Any
    @Inject
    InjectableInstance<ReactiveMongoClient> reactiveMongoClients;

    @PostConstruct
    void init() {
        for (InstanceHandle<MongoClient> handle : mongoClient.handles()) {
            if (handle.getBean().isActive()) {
                String clientName = mongoClientName(handle.getBean());
                MongoClientConfig clientConfig = mongoConfig.clients().get(clientName);
                checks.add(new MongoClientCheck(MongoConfig.nameOrDefault(clientName), handle.get(), clientConfig));
            }
        }

        for (InstanceHandle<ReactiveMongoClient> handle : reactiveMongoClients.handles()) {
            if (handle.getBean().isActive()) {
                String clientName = mongoClientName(handle.getBean());
                MongoClientConfig clientConfig = mongoConfig.clients().get(clientName);
                checks.add(new ReactiveMongoClientCheck(MongoConfig.reactiveNameOrDefault(clientName), handle.get(),
                        clientConfig));
            }
        }
    }

    private BiFunction<Document, Throwable, Tuple2<String, String>> toResult(String name) {
        return new BiFunction<Document, Throwable, Tuple2<String, String>>() {
            @Override
            public Tuple2<String, String> apply(Document ignored, Throwable failure) {
                return Tuple2.of(name, failure == null ? "OK" : failure.getMessage());
            }
        };
    }

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("MongoDB connection health check").up();
        List<Uni<Tuple2<String, String>>> unis = new ArrayList<>();
        for (Supplier<Uni<Tuple2<String, String>>> client : checks) {
            unis.add(client.get());
        }

        if (unis.isEmpty()) {
            return builder.build();
        }

        return Uni.combine().all().unis(unis)
                .collectFailures() // We collect all failures to avoid partial responses.
                .with(new Function<List<?>, HealthCheckResponse>() {
                    @Override
                    public HealthCheckResponse apply(List<?> list) {
                        return MongoHealthCheck.this.combine(list, builder);
                    }
                }).await().indefinitely(); // All checks fail after a timeout, so it won't be forever.

    }

    @SuppressWarnings("unchecked")
    private HealthCheckResponse combine(List<?> results, HealthCheckResponseBuilder builder) {
        for (Object result : results) {
            Tuple2<String, String> tuple = (Tuple2<String, String>) result;
            if ("OK".equalsIgnoreCase(tuple.getItem2())) {
                builder.withData(tuple.getItem1(), "OK");
            } else {
                builder.down()
                        .withData(tuple.getItem1(), "KO, reason: " + tuple.getItem2());
            }
        }
        return builder.build();
    }

    private class MongoClientCheck implements Supplier<Uni<Tuple2<String, String>>> {
        final String name;
        final MongoClient client;
        final MongoClientConfig config;

        MongoClientCheck(String name, MongoClient client, MongoClientConfig config) {
            this.name = name;
            this.client = client;
            this.config = config;
        }

        public Uni<Tuple2<String, String>> get() {
            return Uni.createFrom().item(new Supplier<Document>() {
                @Override
                public Document get() {
                    return client.getDatabase(config.healthDatabase()).runCommand(COMMAND);
                }
            })
                    .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                    .ifNoItem().after(config.readTimeout().orElse(DEFAULT_TIMEOUT)).fail()
                    .onItemOrFailure().transform(toResult(name));
        }
    }

    private class ReactiveMongoClientCheck implements Supplier<Uni<Tuple2<String, String>>> {
        final String name;
        final ReactiveMongoClient client;
        final MongoClientConfig config;

        ReactiveMongoClientCheck(String name, ReactiveMongoClient client, MongoClientConfig config) {
            this.name = name;
            this.client = client;
            this.config = config;
        }

        public Uni<Tuple2<String, String>> get() {
            return client.getDatabase(config.healthDatabase()).runCommand(COMMAND)
                    .ifNoItem().after(config.readTimeout().orElse(DEFAULT_TIMEOUT)).fail()
                    .onItemOrFailure().transform(toResult(name));
        }
    }
}
