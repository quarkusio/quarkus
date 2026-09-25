package io.quarkus.amazon.lambda.http;

import java.util.regex.Pattern;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.value.registry.ValueRegistry;
import io.quarkus.value.registry.ValueRegistry.RuntimeKey;

@Recorder
public class LambdaHttpRecorder {
    /**
     * @deprecated Properly use the config object
     */
    @Deprecated
    static LambdaHttpConfig config;
    /**
     * @deprecated Properly use the config object
     */
    @Deprecated
    static Pattern groupPattern;

    // RuntimeKey for HTTP test port - same key used by TestHTTPResourceManager
    private static final RuntimeKey<Integer> HTTP_TEST_PORT = RuntimeKey.intKey("quarkus.http.test-port");

    private final RuntimeValue<LambdaHttpConfig> runtimeConfig;
    private final RuntimeValue<ValueRegistry> valueRegistry;

    public LambdaHttpRecorder(final RuntimeValue<LambdaHttpConfig> runtimeConfig,
            final RuntimeValue<ValueRegistry> valueRegistry) {
        this.runtimeConfig = runtimeConfig;
        this.valueRegistry = valueRegistry;
    }

    public void setConfig() {
        config = runtimeConfig.getValue();
        groupPattern = Pattern.compile(runtimeConfig.getValue().cognitoClaimMatcher());
    }

    public void registerMockEventServerPort() {
        // Read the actual port from config (set by DevServicesResultBuildItem)
        String portProperty = "quarkus.lambda.mock-event-server.test-port";
        int port = ConfigProvider.getConfig()
                .getOptionalValue(portProperty, Integer.class)
                .orElse(8081); // Default test port

        // Register the port so TestHTTPResourceManager can use it
        valueRegistry.getValue().register(HTTP_TEST_PORT, port);
    }
}
