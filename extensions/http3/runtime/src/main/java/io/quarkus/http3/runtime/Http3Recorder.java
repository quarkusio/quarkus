package io.quarkus.http3.runtime;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class Http3Recorder {

    private static final Logger LOG = Logger.getLogger("io.quarkus.http3");

    public void setCertOrigin(CertOrigin certOrigin) {
        Http3Customizer.certOrigin = certOrigin;
    }

    public void checkTlsAndLogContainerWarning() {
        if (!Http3Customizer.httpsConfigured) {
            throw new IllegalStateException(
                    "HTTP/3 requires TLS but no HTTPS server was configured. "
                            + "Configure quarkus.tls.key-store.* or set quarkus.http3.enabled=false to disable HTTP/3.");
        }
    }
}
