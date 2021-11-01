package io.quarkus.resteasy.reactive.server.runtime;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.jboss.resteasy.reactive.server.core.Deployment;
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
        List<RuntimeConfigurableServerRestHandler> runtimeConfigurableServerRestHandlers = deployment.getValue()
                .getRuntimeConfigurableServerRestHandlers();
        for (RuntimeConfigurableServerRestHandler handler : runtimeConfigurableServerRestHandlers) {
            handler.configure(new RuntimeConfiguration() {
                @Override
                public Duration readTimeout() {
                    return httpConf.readTimeout;
                }

                @Override
                public Body body() {
                    return new Body() {
                        @Override
                        public boolean deleteUploadedFilesOnEnd() {
                            return httpConf.body.deleteUploadedFilesOnEnd;
                        }

                        @Override
                        public String uploadsDirectory() {
                            return httpConf.body.uploadsDirectory;
                        }

                        @Override
                        public Charset defaultCharset() {
                            return runtimeConf.multipart.inputPart.defaultCharset;
                        }
                    };
                }

                @Override
                public Limits limits() {
                    return new Limits() {
                        @Override
                        public Optional<Long> maxBodySize() {
                            if (httpConf.limits.maxBodySize.isPresent()) {
                                return Optional.of(httpConf.limits.maxBodySize.get().asLongValue());
                            } else {
                                return Optional.empty();
                            }
                        }

                        @Override
                        public long maxFormAttributeSize() {
                            return httpConf.limits.maxFormAttributeSize.asLongValue();
                        }
                    };
                }
            });
        }
    }
}
