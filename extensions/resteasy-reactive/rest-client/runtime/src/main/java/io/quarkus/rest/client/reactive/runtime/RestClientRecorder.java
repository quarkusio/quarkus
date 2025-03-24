package io.quarkus.rest.client.reactive.runtime;

import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.rest.client.spi.RestClientBuilderResolver;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class RestClientRecorder {
    private static volatile Map<String, String> configKeys;
    private static volatile Set<String> blockingClassNames;

    private static volatile Map<String, String> blockingMethods;

    public void setConfigKeys(Map<String, String> configKeys) {
        RestClientRecorder.configKeys = configKeys;
    }

    public void setBlockingClassNames(Set<String> blockingClassNames) {
        RestClientRecorder.blockingClassNames = blockingClassNames;
    }

    public static Map<String, String> getConfigKeys() {
        return configKeys;
    }

    public static boolean isClassBlocking(Class<?> exceptionMapperClass) {
        return blockingClassNames.contains(exceptionMapperClass.getName());
    }

    public void setRestClientBuilderResolver() {
        RestClientBuilderResolver.setInstance(new BuilderResolver());
    }

}
