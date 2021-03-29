package io.quarkus.hibernate.orm.runtime.integration;

import java.util.function.BiConsumer;

public interface HibernateOrmIntegrationRuntimeInitListener {

    void contributeRuntimeProperties(BiConsumer<String, Object> propertyCollector);

}
