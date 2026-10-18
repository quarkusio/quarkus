package io.quarkus.test.common;

import java.util.Map;
import java.util.Optional;

/**
 * Interface that can be used to get properties from Dev Services for {@link QuarkusTest} and {@link QuarkusIntegrationTest}
 * based tests.
 *
 * This can be injected into fields on a test class, or injected into {@link ContextAware} objects on the test class
 * or {@link io.quarkus.test.common.TestResourceManager} implementations.
 */
public interface DevServicesContext {

    /**
     * Returns a map containing all the properties created by potentially launched Dev Services.
     * If no Dev Services were launched, the map will be empty.
     */
    Map<String, String> devServicesProperties();

    /**
     * If the application is going to be launched in a container, this method returns the id of the container network
     * it will be launched on.
     */
    Optional<String> containerNetworkId();

    /**
     * Interface that can be implemented to allow automatic injection of the context.
     *
     * If you have a field on a test class that implements this interface then the context
     * will be injected into it.
     *
     * {@link io.quarkus.test.common.QuarkusTestResourceLifecycleManager} implementations can also
     * implement this. This allows for them to set up the resource created by Dev Services after
     * it has been started.
     */
    interface ContextAware {
        void setIntegrationTestContext(DevServicesContext context);
    }
}
