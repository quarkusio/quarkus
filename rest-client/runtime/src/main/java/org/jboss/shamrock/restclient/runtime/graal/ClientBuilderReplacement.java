package org.jboss.shamrock.restclient.runtime.graal;

import javax.ws.rs.client.ClientBuilder;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(ClientBuilder.class)
final class ClientBuilderReplacement {

    @Substitute
    public static ClientBuilder newBuilder() {
        return new ResteasyClientBuilder();
    }
}
