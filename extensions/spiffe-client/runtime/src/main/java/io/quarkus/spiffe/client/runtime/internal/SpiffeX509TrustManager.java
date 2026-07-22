package io.quarkus.spiffe.client.runtime.internal;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

final class SpiffeX509TrustManager implements X509TrustManager {

    private final X509TrustManager delegate;

    SpiffeX509TrustManager(X509TrustManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        validateSpiffeChain(chain);
        delegate.checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        validateSpiffeChain(chain);
        delegate.checkServerTrusted(chain, authType);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return delegate.getAcceptedIssuers();
    }

    private static void validateSpiffeChain(X509Certificate[] chain) throws CertificateException {
        if (chain == null || chain.length == 0) {
            throw new CertificateException("Empty certificate chain");
        }
        try {
            SpiffeValidator.validateLeaf(chain[0]);
            for (int i = 1; i < chain.length; i++) {
                SpiffeValidator.validateIntermediate(chain[i]);
            }
        } catch (Exception e) {
            throw new CertificateException("SPIFFE X.509-SVID validation failed: " + e.getMessage(), e);
        }
    }
}
