package io.quarkus.hibernate.orm.runtime.service.propertyaccessor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.accessor.AccessorFactory;
import org.hibernate.accessor.spi.AccessorConfiguration;
import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.accessor.runtime.QuarkusAccessContext;
import io.quarkus.runtime.RuntimeValue;

class QuarkusPropertyAccessorServiceTest {

    @Test
    void usesConfiguredFactoryWithoutStaticInitialization() throws Exception {
        AccessorFactory factory = AccessorFactory.reflection(new AccessorConfiguration(new QuarkusAccessContext(), Map.of()));
        var service = QuarkusPropertyAccessorServiceInitiator.INSTANCE.initiateService(
                Map.of(QuarkusPropertyAccessorServiceInitiator.ACCESSOR_FACTORY, factory), null);
        AccessorFactory ormFactory = service.hibernateAccessorFactory();

        TestEntity entity = ormFactory.instantiator(TestEntity.class.getConstructor()).create();
        var field = TestEntity.class.getField("name");
        ormFactory.valueWriter(field).set(entity, "configured factory");
        assertThat(ormFactory.valueReader(field).get(entity)).isEqualTo("configured factory");
    }

    @Test
    void contributesFactoryToBootAndRuntimeSettings() {
        AccessorFactory factory = AccessorFactory.reflection(new AccessorConfiguration(new QuarkusAccessContext(), Map.of()));
        var integration = new QuarkusPropertyAccessorIntegration(new RuntimeValue<>(factory));
        Map<String, Object> bootSettings = new HashMap<>();
        Map<String, Object> runtimeSettings = new HashMap<>();

        integration.contributeBootProperties(bootSettings::put);
        integration.onMetadataInitialized(null, null, runtimeSettings::put);

        assertThat(bootSettings.get(QuarkusPropertyAccessorServiceInitiator.ACCESSOR_FACTORY)).isSameAs(factory);
        assertThat(runtimeSettings.get(QuarkusPropertyAccessorServiceInitiator.ACCESSOR_FACTORY)).isSameAs(factory);
    }

    public static class TestEntity {
        public String name;
    }
}
