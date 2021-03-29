package io.quarkus.smallrye.opentracing.runtime;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

import io.opentracing.Tracer;
import io.opentracing.contrib.jaxrs2.server.OperationNameProvider;
import io.opentracing.contrib.jaxrs2.server.ServerTracingDynamicFeature;

@Provider
public class QuarkusSmallRyeTracingDynamicFeature implements DynamicFeature {
    @Inject
    TracingConfig tracingConfig;

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        ServerTracingDynamicFeature.Builder builder = new ServerTracingDynamicFeature.Builder(
                CDI.current().select(Tracer.class).get())
                        .withOperationNameProvider(OperationNameProvider.ClassNameOperationName.newBuilder())
                        .withTraceSerialization(false);

        tracingConfig.skipPattern.ifPresent(builder::withSkipPattern);
        if (tracingConfig.operationNameProvider.isPresent()) {
            if (tracingConfig.operationNameProvider.get().equals(TracingConfig.OperationNameProvider.HTTP_PATH)) {
                builder.withOperationNameProvider(OperationNameProvider.WildcardOperationName.newBuilder());
            }
        }

        ServerTracingDynamicFeature serverTracing = builder.build();
        serverTracing.configure(resourceInfo, context);
    }
}
