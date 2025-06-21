package io.quarkus.resteasy.reactive.server.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.spi.DefaultRuntimeConfiguration;
import org.jboss.resteasy.reactive.server.spi.GenericRuntimeConfigurableServerRestHandler;
import org.jboss.resteasy.reactive.server.spi.RuntimeConfiguration;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;

@Recorder
public class ResteasyReactiveRuntimeRecorder {

    private final RuntimeValue<ResteasyReactiveServerRuntimeConfig> runtimeConfig;
    private final RuntimeValue<VertxHttpConfig> httpRuntimeConfig;

    public ResteasyReactiveRuntimeRecorder(
            final RuntimeValue<ResteasyReactiveServerRuntimeConfig> runtimeConfig,
            final RuntimeValue<VertxHttpConfig> httpRuntimeConfig) {
        this.runtimeConfig = runtimeConfig;
        this.httpRuntimeConfig = httpRuntimeConfig;
    }

    public Supplier<RuntimeConfiguration> runtimeConfiguration(RuntimeValue<Deployment> deployment) {
        ResteasyReactiveServerRuntimeConfig runtimeConfig = this.runtimeConfig.getValue();
        VertxHttpConfig httpRuntimeConfig = this.httpRuntimeConfig.getValue();

        Optional<Long> maxBodySize;
        if (httpRuntimeConfig.limits().maxBodySize().isPresent()) {
            maxBodySize = Optional.of(httpRuntimeConfig.limits().maxBodySize().get().asLongValue());
        } else {
            maxBodySize = Optional.empty();
        }

        RuntimeConfiguration runtimeConfiguration = new DefaultRuntimeConfiguration(httpRuntimeConfig.readTimeout(),
                httpRuntimeConfig.body().deleteUploadedFilesOnEnd(), httpRuntimeConfig.body().uploadsDirectory(),
                httpRuntimeConfig.body().multipart().fileContentTypes().orElse(null),
                runtimeConfig.multipart().inputPart().defaultCharset(), maxBodySize,
                httpRuntimeConfig.limits().maxFormAttributeSize().asLongValue(),
                httpRuntimeConfig.limits().maxParameters());

        deployment.getValue().setRuntimeConfiguration(runtimeConfiguration);

        return new Supplier<>() {
            @Override
            public RuntimeConfiguration get() {
                return runtimeConfiguration;
            }
        };
    }

    @SuppressWarnings({ "unchecked", "rawtypes", "ForLoopReplaceableByForEach" })
    public void configureHandlers(RuntimeValue<Deployment> deployment, Map<Class<?>, Supplier<?>> runtimeConfigMap) {
        List<GenericRuntimeConfigurableServerRestHandler<?>> runtimeConfigurableServerRestHandlers = deployment.getValue()
                .getRuntimeConfigurableServerRestHandlers();
        for (int i = 0; i < runtimeConfigurableServerRestHandlers.size(); i++) {
            GenericRuntimeConfigurableServerRestHandler handler = runtimeConfigurableServerRestHandlers.get(i);
            Supplier<?> supplier = runtimeConfigMap.get(handler.getConfigurationClass());
            if (supplier == null) {
                throw new IllegalStateException(
                        "Handler '" + handler.getClass().getName() + "' has not been properly configured.");
            }
            handler.configure(supplier.get());
        }
    }
}
