package io.quarkus.http3.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.http3.runtime.CertOrigin;

/**
 * Build item that store the origin of the certificate used by the HTTP/3 server.
 */
final class Http3CertOriginBuildItem extends SimpleBuildItem {
    private final CertOrigin certOrigin;

    Http3CertOriginBuildItem(CertOrigin certOrigin) {
        this.certOrigin = certOrigin;
    }

    public CertOrigin getCertOrigin() {
        return certOrigin;
    }
}
