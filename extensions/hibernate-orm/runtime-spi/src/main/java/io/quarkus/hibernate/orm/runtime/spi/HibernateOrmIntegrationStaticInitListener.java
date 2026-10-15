package io.quarkus.hibernate.orm.runtime.spi;

import java.util.function.BiConsumer;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;

public interface HibernateOrmIntegrationStaticInitListener {

    void contributeBootProperties(BiConsumer<String, Object> propertyCollector);

    void onMetadataInitialized(Metadata metadata, BootstrapContext bootstrapContext,
            BiConsumer<String, Object> propertyCollector);

}
