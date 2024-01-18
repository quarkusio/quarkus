package io.quarkus.restclient.runtime.graal;

import jakarta.ws.rs.client.ClientBuilder;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.internal.LocalResteasyProviderFactory;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.restclient.runtime.RestClientRecorder;

@TargetClass(ClientBuilder.class)
final class ClientBuilderReplacement {

    @Substitute
    public static ClientBuilder newBuilder() {
        ResteasyClientBuilder client = new ResteasyClientBuilderImpl();
        client.providerFactory(new LocalResteasyProviderFactory(RestClientRecorder.providerFactory));
        return client;
    }
}
