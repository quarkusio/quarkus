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
import io.quarkus.vertx.http.runtime.HttpConfiguration;

@Recorder
public class ResteasyReactiveRuntimeRecorder {

    final HttpConfiguration httpConf;

    public ResteasyReactiveRuntimeRecorder(HttpConfiguration httpConf) {
        this.httpConf = httpConf;
    }

    public Supplier<RuntimeConfiguration> runtimeConfiguration(RuntimeValue<Deployment> deployment,
            ResteasyReactiveServerRuntimeConfig runtimeConf) {
        Optional<Long> maxBodySize;

        if (httpConf.limits.maxBodySize.isPresent()) {
            maxBodySize = Optional.of(httpConf.limits.maxBodySize.get().asLongValue());
        } else {
            maxBodySize = Optional.empty();
        }

        RuntimeConfiguration runtimeConfiguration = new DefaultRuntimeConfiguration(httpConf.readTimeout,
                httpConf.body.deleteUploadedFilesOnEnd, httpConf.body.uploadsDirectory,
                httpConf.body.multipart.fileContentTypes.orElse(null),
                runtimeConf.multipart().inputPart().defaultCharset(), maxBodySize,
                httpConf.limits.maxFormAttributeSize.asLongValue());

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
