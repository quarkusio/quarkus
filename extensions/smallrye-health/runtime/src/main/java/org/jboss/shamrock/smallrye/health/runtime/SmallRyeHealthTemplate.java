package org.jboss.shamrock.smallrye.health.runtime;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.spi.HealthCheckResponseProvider;
import org.jboss.shamrock.runtime.annotations.Template;

@Template
public class SmallRyeHealthTemplate {

    public void registerHealthCheckResponseProvider(Class<? extends HealthCheckResponseProvider> providerClass) {
        try {
            HealthCheckResponse.setResponseProvider(providerClass.getConstructor().newInstance());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to instantiate service " + providerClass + " using the no-arg constructor.");
        }
    }

}
