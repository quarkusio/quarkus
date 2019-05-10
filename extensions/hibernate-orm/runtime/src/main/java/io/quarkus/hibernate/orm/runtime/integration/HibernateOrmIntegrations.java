package io.quarkus.hibernate.orm.runtime.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.BootstrapContext;

public class HibernateOrmIntegrations {

    private static final List<HibernateOrmIntegrationListener> LISTENERS = new ArrayList<>();

    public static void registerListener(HibernateOrmIntegrationListener listener) {
        LISTENERS.add(listener);
    }

    public static void contributeBootProperties(BiConsumer<String, Object> propertyCollector) {
        for (HibernateOrmIntegrationListener listener : LISTENERS) {
            listener.contributeBootProperties(propertyCollector);
        }
    }

    public static void onMetadataInitialized(Metadata metadata, BootstrapContext bootstrapContext,
            BiConsumer<String, Object> propertyCollector) {
        for (HibernateOrmIntegrationListener listener : LISTENERS) {
            ClassLoaderService cls = bootstrapContext.getServiceRegistry().getService(ClassLoaderService.class);
            listener.onMetadataInitialized(metadata, bootstrapContext, propertyCollector);
        }
    }

    public static void contributeRuntimeProperties(BiConsumer<String, Object> propertyCollector) {
        for (HibernateOrmIntegrationListener listener : LISTENERS) {
            listener.contributeRuntimeProperties(propertyCollector);
        }
    }
}
