package io.quarkus.it.extension.configurable;

import java.util.Map;

import io.quarkus.it.extension.Counter;
import io.quarkus.test.common.QuarkusTestResourceConfigurableLifecycleManager;

public class ConfigurableLifecycleManager
        implements QuarkusTestResourceConfigurableLifecycleManager<CustomResourceWithAttribute> {

    String attributeValue;

    @Override
    public void init(CustomResourceWithAttribute annotation) {
        attributeValue = annotation.value();
    }

    @Override
    public Map<String, String> start() {
        Counter.startCounter.incrementAndGet();
        return Map.of("attributeValue", attributeValue);
    }

    @Override
    public void stop() {
        Counter.endCounter.incrementAndGet();
    }

}
