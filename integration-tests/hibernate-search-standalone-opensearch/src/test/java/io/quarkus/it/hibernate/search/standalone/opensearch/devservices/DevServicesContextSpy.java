package io.quarkus.it.hibernate.search.standalone.opensearch.devservices;

import java.util.Collections;
import java.util.Map;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class DevServicesContextSpy implements QuarkusTestResourceLifecycleManager, DevServicesContext.ContextAware {

    DevServicesContext devServicesContext;

    @Override
    public void setIntegrationTestContext(DevServicesContext context) {
        this.devServicesContext = context;
    }

    @Override
    public Map<String, String> start() {
        return Collections.emptyMap();
    }

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(devServicesContext, f -> f.getType().isAssignableFrom(DevServicesContext.class));
    }

    @Override
    public void stop() {

    }
}
