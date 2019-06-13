package io.quarkus.smallrye.restclient.runtime.graal;

import javax.ws.rs.client.ClientBuilder;

import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(ClientBuilder.class)
final class ClientBuilderReplacement {

    @Substitute
    public static ClientBuilder newBuilder() {
        return new ResteasyClientBuilderImpl();
    }
}
