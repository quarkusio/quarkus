package io.quarkus.hibernate.envers;

import java.util.function.BiConsumer;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.envers.configuration.EnversSettings;

import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationListener;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrations;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class HibernateEnversRecorder {

    public void registerHibernateEnversIntegration(HibernateEnversBuildTimeConfig buildTimeConfig) {
        HibernateOrmIntegrations.registerListener(new HibernateEnversIntegrationListener(buildTimeConfig));
    }

    private static final class HibernateEnversIntegrationListener implements HibernateOrmIntegrationListener {

        private HibernateEnversBuildTimeConfig buildTimeConfig;

        private HibernateEnversIntegrationListener(HibernateEnversBuildTimeConfig buildTimeConfig) {
            this.buildTimeConfig = buildTimeConfig;
        }

        @Override
        public void contributeBootProperties(BiConsumer<String, Object> propertyCollector) {
            addConfig(propertyCollector, EnversSettings.STORE_DATA_AT_DELETE, buildTimeConfig.storeDataAtDelete);
        }

        public static <T> void addConfig(BiConsumer<String, Object> propertyCollector, String configPath, T value) {
            propertyCollector.accept(configPath, value);
        }

        @Override
        public void onMetadataInitialized(Metadata metadata, BootstrapContext bootstrapContext,
                BiConsumer<String, Object> propertyCollector) {
        }

        @Override
        public void contributeRuntimeProperties(BiConsumer<String, Object> propertyCollector) {
        }
    }
}
