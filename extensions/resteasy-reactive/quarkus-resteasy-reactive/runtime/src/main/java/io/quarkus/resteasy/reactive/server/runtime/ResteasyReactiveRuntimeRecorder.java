package io.quarkus.resteasy.reactive.server.runtime;

import java.util.List;
import java.util.Optional;

import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.spi.DefaultRuntimeConfiguration;
import org.jboss.resteasy.reactive.server.spi.RuntimeConfigurableServerRestHandler;
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

    public void configure(RuntimeValue<Deployment> deployment,
            ResteasyReactiveServerRuntimeConfig runtimeConf) {
        Optional<Long> maxBodySize;

        if (httpConf.limits.maxBodySize.isPresent()) {
            maxBodySize = Optional.of(httpConf.limits.maxBodySize.get().asLongValue());
        } else {
            maxBodySize = Optional.empty();
        }
        RuntimeConfiguration runtimeConfiguration = new DefaultRuntimeConfiguration(httpConf.readTimeout,
                httpConf.body.deleteUploadedFilesOnEnd, httpConf.body.uploadsDirectory,
                runtimeConf.multipart.inputPart.defaultCharset, maxBodySize,
                httpConf.limits.maxFormAttributeSize.asLongValue());

        List<RuntimeConfigurableServerRestHandler> runtimeConfigurableServerRestHandlers = deployment.getValue()
                .getRuntimeConfigurableServerRestHandlers();
        deployment.getValue().setRuntimeConfiguration(runtimeConfiguration);
        for (int i = 0; i < runtimeConfigurableServerRestHandlers.size(); i++) {
            runtimeConfigurableServerRestHandlers.get(i).configure(runtimeConfiguration);
        }
    }
}
