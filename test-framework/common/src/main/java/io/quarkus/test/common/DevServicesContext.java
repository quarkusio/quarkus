package io.quarkus.test.common;

import java.util.Map;

/**
 * Interface that can be used to get properties from DevServices for {@link QuarkusTest} and {@link QuarkusIntegrationTest}
 * based tests.
 *
 * This can be injected into fields on a test class, or injected into {@link ContextAware} objects on the test class
 * or {@link io.quarkus.test.common.TestResourceManager} implementations.
 *
 */
public interface DevServicesContext {

    /**
     * Returns a map containing all the properties creates by potentially launched dev services.
     * If no dev services where launched, the map will be empty.
     */
    Map<String, String> devServicesProperties();

    /**
     * Interface that can be implemented to allow automatic injection of the context.
     *
     * If you have a field on a test class that implements this interface the then context
     * will be injected into it.
     *
     * {@link io.quarkus.test.common.QuarkusTestResourceLifecycleManager} implementations can also
     * implement this. This allows for them to setup the resource created by Dev Services after
     * it has been started.
     *
     */
    interface ContextAware {
        void setIntegrationTestContext(DevServicesContext context);
    }
}
