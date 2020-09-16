package io.quarkus.restclient.runtime.graal;

import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * This class is used to make sure that only the default SSLContext is requested when no SSLContext has been provided.
 * The reason this is necessary is because by default, the code requests
 * {@code SSLContext.getInstance(SSLConnectionSocketFactory.TLS)} which will fail in native when the SSL has been disabled
 */
@TargetClass(className = "org.jboss.resteasy.client.jaxrs.engines.ClientHttpEngineBuilder43")
public final class ClientHttpEngineBuilder43Replacement {

    @Alias
    private ResteasyClientBuilder that;

    @Substitute
    public ClientHttpEngineBuilder43Replacement resteasyClientBuilder(ResteasyClientBuilder resteasyClientBuilder) {
        that = resteasyClientBuilder;
        // make sure we only set a context if there is none or one wouldn't be created implicitly
        if ((that.getSSLContext() == null) && (that.getTrustStore() == null) && (that.getKeyStore() == null)) {
            try {
                that.sslContext(SSLContext.getDefault());
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        return this;
    }
}
