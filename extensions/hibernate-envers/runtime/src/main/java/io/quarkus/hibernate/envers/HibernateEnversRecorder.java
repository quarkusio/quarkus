package io.quarkus.hibernate.envers;

import java.util.function.BiConsumer;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.envers.configuration.EnversSettings;

import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationStaticInitListener;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class HibernateEnversRecorder {

    public HibernateOrmIntegrationStaticInitListener createStaticInitListener(HibernateEnversBuildTimeConfig buildTimeConfig) {
        return new HibernateEnversIntegrationListener(buildTimeConfig);
    }

    private static final class HibernateEnversIntegrationListener implements HibernateOrmIntegrationStaticInitListener {
        private HibernateEnversBuildTimeConfig buildTimeConfig;

        private HibernateEnversIntegrationListener(HibernateEnversBuildTimeConfig buildTimeConfig) {
            this.buildTimeConfig = buildTimeConfig;
        }

        @Override
        public void contributeBootProperties(BiConsumer<String, Object> propertyCollector) {
            addConfig(propertyCollector, EnversSettings.STORE_DATA_AT_DELETE, buildTimeConfig.storeDataAtDelete);
            addConfig(propertyCollector, EnversSettings.AUDIT_TABLE_SUFFIX, buildTimeConfig.auditTableSuffix);
        }

        public static <T> void addConfig(BiConsumer<String, Object> propertyCollector, String configPath, T value) {
            propertyCollector.accept(configPath, value);
        }

        @Override
        public void onMetadataInitialized(Metadata metadata, BootstrapContext bootstrapContext,
                BiConsumer<String, Object> propertyCollector) {
        }
    }
}
