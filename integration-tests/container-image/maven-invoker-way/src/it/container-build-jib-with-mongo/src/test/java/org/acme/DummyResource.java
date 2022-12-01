package org.acme;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class DummyResource implements QuarkusTestResourceLifecycleManager, DevServicesContext.ContextAware {

    public static final AtomicReference<Optional<String>> CONTAINER_NETWORK_ID = new AtomicReference<>(null);

    @Override
    public Map<String, String> start() {
        return Collections.emptyMap();
    }

    @Override
    public void stop() {

    }


    @Override
    public void setIntegrationTestContext(DevServicesContext context) {
        CONTAINER_NETWORK_ID.set(context.containerNetworkId());
    }
}
