
package io.quarkus.tck.opentracing;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.ws.rs.client.ClientBuilder;

import org.eclipse.microprofile.opentracing.ClientTracingRegistrarProvider;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedExecutorService;
import io.smallrye.opentracing.SmallRyeClientTracingFeature;

public class ResteasyClientTracingRegistrarProvider implements ClientTracingRegistrarProvider {

    public ClientBuilder configure(ClientBuilder clientBuilder) {
        return configure(clientBuilder, Executors.newFixedThreadPool(10));
    }

    public ClientBuilder configure(ClientBuilder clientBuilder, ExecutorService executorService) {
        ResteasyClientBuilder resteasyClientBuilder = (ResteasyClientBuilder) clientBuilder;
        Tracer tracer = CDI.current().select(Tracer.class).get();
        return resteasyClientBuilder.executorService(new TracedExecutorService(executorService, tracer))
                .register(new SmallRyeClientTracingFeature(tracer));
    }
}
