package io.quarkus.vertx.http.runtime.attribute;

import java.util.Base64;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.cert.CertificateEncodingException;
import javax.security.cert.X509Certificate;

import io.vertx.ext.web.RoutingContext;

public class SslClientCertAttribute implements ExchangeAttribute {

    public static final SslClientCertAttribute INSTANCE = new SslClientCertAttribute();
    public static final java.lang.String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";

    public static final java.lang.String END_CERT = "-----END CERTIFICATE-----";

    public static String toPem(final X509Certificate certificate) throws CertificateEncodingException {
        final StringBuilder builder = new StringBuilder();
        builder.append(BEGIN_CERT);
        builder.append('\n');
        builder.append(Base64.getEncoder().encodeToString(certificate.getEncoded()));
        builder.append('\n');
        builder.append(END_CERT);
        return builder.toString();
    }

    @Override
    public String readAttribute(RoutingContext exchange) {
        SSLSession ssl = exchange.request().sslSession();
        if (ssl == null) {
            return null;
        }
        X509Certificate[] certificates;
        try {
            certificates = ssl.getPeerCertificateChain();
            if (certificates.length > 0) {
                return toPem(certificates[0]);
            }
            return null;
        } catch (SSLPeerUnverifiedException e) {
            return null;
        } catch (CertificateEncodingException e) {
            return null;
        }
    }

    @Override
    public void writeAttribute(RoutingContext exchange, String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException("SSL Client Cert", newValue);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return "SSL Client Cert";
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.equals("%{SSL_CLIENT_CERT}")) {
                return INSTANCE;
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
