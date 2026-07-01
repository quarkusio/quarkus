package io.quarkus.flyway.mongodb.runtime;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.inject.Default;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.flyway.mongodb.FlywayMongodbClient;
import io.quarkus.mongodb.runtime.MongoConfig;

public final class FlywayMongodbContainerUtil {

    private FlywayMongodbContainerUtil() {
    }

    public static Annotation getQualifier(String clientName) {
        if (MongoConfig.isDefaultClient(clientName)) {
            return Default.Literal.INSTANCE;
        }
        return FlywayMongodbClient.Literal.of(clientName);
    }

    public static FlywayMongodbContainer getFlywayMongodbContainer(String clientName) {
        Annotation qualifier = getQualifier(clientName);
        InjectableInstance<FlywayMongodbContainer> instance = Arc.container()
                .select(FlywayMongodbContainer.class, qualifier);
        if (!instance.isResolvable()) {
            return null;
        }
        if (!instance.getHandle().getBean().isActive()) {
            return null;
        }
        return instance.get();
    }

    public static List<FlywayMongodbContainer> getActiveFlywayMongodbContainers() {
        List<FlywayMongodbContainer> containers = new ArrayList<>();
        InjectableInstance<FlywayMongodbContainer> all = Arc.container().select(FlywayMongodbContainer.class);
        for (InstanceHandle<FlywayMongodbContainer> handle : all.handles()) {
            if (handle.getBean().isActive()) {
                containers.add(handle.get());
            }
        }
        return containers;
    }
}
