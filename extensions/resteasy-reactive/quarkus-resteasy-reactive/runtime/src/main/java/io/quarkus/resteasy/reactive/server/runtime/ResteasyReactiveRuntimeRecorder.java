package io.quarkus.resteasy.reactive.server.runtime;

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

    public void configure(RuntimeValue<Deployment> deployment, HttpConfiguration configuration) {
        List<RuntimeConfigurableServerRestHandler> runtimeConfigurableServerRestHandlers = deployment.getValue()
                .getRuntimeConfigurableServerRestHandlers();
        for (RuntimeConfigurableServerRestHandler handler : runtimeConfigurableServerRestHandlers) {
            handler.configure(new RuntimeConfiguration() {
                @Override
                public Duration readTimeout() {
                    return configuration.readTimeout;
                }

                @Override
                public Body body() {
                    return new Body() {
                        @Override
                        public boolean deleteUploadedFilesOnEnd() {
                            return configuration.body.deleteUploadedFilesOnEnd;
                        }

                        @Override
                        public String uploadsDirectory() {
                            return configuration.body.uploadsDirectory;
                        }
                    };
                }

                @Override
                public Limits limits() {
                    return new Limits() {
                        @Override
                        public Optional<Long> maxBodySize() {
                            if (configuration.limits.maxBodySize.isPresent()) {
                                return Optional.of(configuration.limits.maxBodySize.get().asLongValue());
                            } else {
                                return Optional.empty();
                            }
                        }

                        @Override
                        public long maxFormAttributeSize() {
                            return configuration.limits.maxFormAttributeSize.asLongValue();
                        }
                    };
                }
            });
        }
    }
}
