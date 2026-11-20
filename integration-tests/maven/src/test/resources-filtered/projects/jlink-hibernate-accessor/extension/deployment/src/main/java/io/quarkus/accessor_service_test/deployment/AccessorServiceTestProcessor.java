package io.quarkus.accessor_service_test.deployment;

import jakarta.inject.Singleton;

import org.hibernate.accessor.AccessorFactory;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.hibernate.accessor.deployment.HibernateAccessorFactoryBuildItem;

// we do not need this bean for regular apps, but in this case since we want to test
// the accessor factory directly it is easier to expose it through a bean than trying
// to recosutruct another one by hand in /app/src/main/java/org/acme/accessor/AccessorResource.java
class AccessorServiceTestProcessor {
    @BuildStep
    SyntheticBeanBuildItem factoryBean(HibernateAccessorFactoryBuildItem factory) {
        // The consumed build item orders this step after registration of the accessor service.
        return SyntheticBeanBuildItem.configure(AccessorFactory.class)
                .scope(Singleton.class)
                .serviceValue(AccessorFactory.class)
                .done();
    }
}
