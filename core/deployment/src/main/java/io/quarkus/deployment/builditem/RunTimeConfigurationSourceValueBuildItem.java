package io.quarkus.deployment.builditem;

import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * This is a special build item that is intended to be used only to support bootstrap configuration in the following manner:
 *
 * A build step returns this build item (this is a limitation compared to other build items that can also be used with
 * BuildProducer)
 * containing a {@code RuntimeValue<ConfigSourceProvider>} that is obtained by calling a ({@code RUNTIME_INIT}) recorder.
 * The build step can optionally use a configuration object that uses the {@code BOOTSTRAP} config phase and pass this
 * configuration
 * to the recorder to allow the recorder at runtime to customize its behavior
 */
public final class RunTimeConfigurationSourceValueBuildItem extends MultiBuildItem {

    private final RuntimeValue<ConfigSourceProvider> configSourcesValue;

    public RunTimeConfigurationSourceValueBuildItem(RuntimeValue<ConfigSourceProvider> configSourcesValue) {
        this.configSourcesValue = configSourcesValue;
    }

    public RuntimeValue<ConfigSourceProvider> getConfigSourcesValue() {
        return configSourcesValue;
    }
}
