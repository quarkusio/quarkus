package io.quarkus.gcp.services.common.runtime.graal;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "com.google.cloud.http.HttpTransportOptions$DefaultHttpTransportFactory")
public final class DefaultHttpTransportFactorySubstitution {
    @Substitute
    public HttpTransport create() {
        // Appengine HttpTransport didn't works on native image.
        // Anyway, appengine don't allow to deploy native image on it so it's not an issue.
        return new NetHttpTransport();
    }
}
