package io.quarkus.spiffe.client.runtime.internal.tls;

import java.net.Socket;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;

import org.jboss.logging.Logger;

import io.netty.handler.ssl.ResumableX509ExtendedTrustManager;
import io.quarkus.spiffe.client.SpiffeConnectionException;
import io.quarkus.spiffe.client.runtime.internal.SpiffeValidator;
import io.quarkus.spiffe.svid.x509.WorkloadCertificateDocument;
import io.quarkus.spiffe.svid.x509.X509Svid;
import io.vertx.core.Vertx;
import io.vertx.core.net.TrustOptions;

// X509ExtendedTrustManager on purpose: the JSSE wraps a plain X509TrustManager with its own hostname verification,
// which no X.509-SVID passes
final class SpiffeTrustManager extends X509ExtendedTrustManager implements TrustOptions, ResumableX509ExtendedTrustManager {

    private static final Logger LOG = Logger.getLogger(SpiffeTrustManager.class);
    private static final String ANY_EXTENDED_KEY_USAGE = "2.5.29.37.0";

    private enum Peer {
        CLIENT("client", "1.3.6.1.5.5.7.3.2"), // this workload is the server
        SERVER("server", "1.3.6.1.5.5.7.3.1"); // this workload is the client

        private final String description;
        private final String extendedKeyUsage;

        Peer(String description, String extendedKeyUsage) {
            this.description = description;
            this.extendedKeyUsage = extendedKeyUsage;
        }
    }

    private final X509SvidCachingClient x509SvidCachingClient;
    private final String tlsConfigurationName;
    private final String spiffeId;

    SpiffeTrustManager(X509SvidCachingClient x509SvidCachingClient, String tlsConfigurationName, String spiffeId) {
        this.x509SvidCachingClient = x509SvidCachingClient;
        this.tlsConfigurationName = tlsConfigurationName;
        this.spiffeId = spiffeId;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        validate(chain, Peer.CLIENT);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        validate(chain, Peer.SERVER);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
            throws CertificateException {
        requireTls13(handshakeSession(socket), Peer.CLIENT);
        validate(chain, Peer.CLIENT);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
            throws CertificateException {
        requireTls13(handshakeSession(socket), Peer.SERVER);
        validate(chain, Peer.SERVER);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
            throws CertificateException {
        requireTls13(engine.getHandshakeSession(), Peer.CLIENT);
        validate(chain, Peer.CLIENT);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
            throws CertificateException {
        requireTls13(engine.getHandshakeSession(), Peer.SERVER);
        validate(chain, Peer.SERVER);
    }

    @Override
    public void resumeClientTrusted(X509Certificate[] chain, SSLEngine engine) throws CertificateException {
        requireWorkloadCertificate(engine, Peer.CLIENT);
    }

    @Override
    public void resumeServerTrusted(X509Certificate[] chain, SSLEngine engine) throws CertificateException {
        requireWorkloadCertificate(engine, Peer.SERVER);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        // no certificate_authorities hint: a SPIFFE peer presents its SVID regardless, trust is decided in validate()
        return new X509Certificate[0];
    }

    @Override
    public TrustOptions copy() {
        // Vert.x caches SSL contexts by options equality, the same instance keeps the cache stable
        return this;
    }

    @Override
    public TrustManagerFactory getTrustManagerFactory(Vertx vertx) throws Exception {
        return TrustOptions.wrap(this).getTrustManagerFactory(vertx);
    }

    @Override
    public Function<String, TrustManager[]> trustManagerMapper(Vertx vertx) {
        // no SNI, Vert.x falls back to getTrustManagerFactory
        return serverName -> null;
    }

    @Override
    public String toString() {
        return "SpiffeTrustManager[" + tlsConfigurationName + ", " + spiffeId + "]";
    }

    private void validate(X509Certificate[] chain, Peer peer) throws CertificateException {
        if (chain == null || chain.length == 0) {
            throw reject(peer, "it presented no certificate", null);
        }
        X509Certificate leaf = chain[0];

        // X.509-SVID 5.2: leaf validation and exactly one URI SAN with a valid SPIFFE ID
        String peerSpiffeId;
        try {
            peerSpiffeId = SpiffeValidator.validateLeaf(leaf);
            SpiffeValidator.validateSpiffeId(peerSpiffeId);
        } catch (SpiffeConnectionException e) {
            throw reject(peer, "its certificate is not a valid X.509-SVID: " + e.getMessage(), e);
        }
        // X.509-SVID 4.4: when the extension is present, it must allow the TLS role of the peer
        List<String> extendedKeyUsage = leaf.getExtendedKeyUsage();
        if (extendedKeyUsage != null && !extendedKeyUsage.contains(ANY_EXTENDED_KEY_USAGE)
                && !extendedKeyUsage.contains(peer.extendedKeyUsage)) {
            throw reject(peer, "the extended key usage of its certificate does not allow TLS " + peer.description
                    + " authentication", null);
        }

        WorkloadCertificateDocument document = x509SvidCachingClient.getCurrentWorkloadCertificate();
        if (document == null) {
            throw reject(peer, "no X.509-SVID trust bundle is available", null);
        }
        // SPIFFE Workload API 4.6: validate against the bundle of the trust domain of the peer; a peer whose trust
        // domain has no bundle is untrusted, and only the bundle of the workload trust domain is available
        String peerTrustDomain = trustDomainOf(peerSpiffeId);
        String trustDomain = trustDomainOf(document.subject());
        if (!peerTrustDomain.equals(trustDomain)) {
            throw reject(peer, "there is no trust bundle for its trust domain '" + peerTrustDomain
                    + "', only the trust domain '" + trustDomain + "' is trusted", null);
        }
        // X.509-SVID 5.1: RFC 5280 path validation against the trust bundle
        validatePath(chain, document.trustBundle().certificates(), peer);

        if (!X509Svid.SAME_TRUST_DOMAIN.equals(spiffeId) && !spiffeId.equals(peerSpiffeId)) {
            LOG.warnf("The TLS configuration '%s' rejected the %s with the SPIFFE ID '%s', it only authorizes '%s'",
                    tlsConfigurationName, peer.description, peerSpiffeId, spiffeId);
            throw new CertificateException("The " + peer.description + " SPIFFE ID '" + peerSpiffeId
                    + "' is not authorized by the TLS configuration '" + tlsConfigurationName + "'");
        }
        LOG.debugf("The TLS configuration '%s' authorized the %s with the SPIFFE ID '%s'", tlsConfigurationName,
                peer.description, peerSpiffeId);
    }

    // a resumed handshake calls no manager, Netty calls the resume methods instead
    // the peer chain is not validated again, a JSSE client would offer a refused session again until it times out
    private void requireWorkloadCertificate(SSLEngine engine, Peer peer) throws CertificateException {
        if (x509SvidCachingClient.getCurrentWorkloadCertificate() == null) {
            engine.getSession().invalidate(); // the next handshake is a full one
            throw reject(peer, "no X.509-SVID is available, the session is not resumed", null);
        }
    }

    private void validatePath(X509Certificate[] chain, List<X509Certificate> trustBundle, Peer peer)
            throws CertificateException {
        Set<TrustAnchor> trustAnchors = new HashSet<>();
        for (X509Certificate ca : trustBundle) {
            trustAnchors.add(new TrustAnchor(ca, null));
        }
        // a peer may send a CA certificate of the bundle along; PKIX wants the path without its trust anchor
        List<X509Certificate> path = new ArrayList<>(chain.length);
        for (X509Certificate certificate : chain) {
            if (trustBundle.contains(certificate)) {
                break;
            }
            path.add(certificate);
        }
        if (path.isEmpty()) {
            throw reject(peer, "it presented a trust bundle CA certificate as its own certificate", null);
        }
        try {
            PKIXParameters parameters = new PKIXParameters(trustAnchors);
            parameters.setRevocationEnabled(false);
            CertPathValidator.getInstance("PKIX")
                    .validate(CertificateFactory.getInstance("X.509").generateCertPath(path), parameters);
        } catch (GeneralSecurityException e) {
            throw reject(peer, "its certificate chain does not chain to the trust bundle: " + e.getMessage(), e);
        }
    }

    // TLS 1.3 only: TLS 1.2 sends the certificates in clear, so the SPIFFE IDs of both workloads are visible on the
    // path; TLS 1.3 encrypts them and has no renegotiation and no legacy key exchanges
    private void requireTls13(SSLSession handshakeSession, Peer peer) throws CertificateException {
        String protocol = handshakeSession == null ? null : handshakeSession.getProtocol();
        if (!"TLSv1.3".equals(protocol)) {
            throw reject(peer, protocol == null ? "the negotiated protocol version is unknown, TLS 1.3 is required"
                    : "the connection negotiated " + protocol + ", TLS 1.3 is required", null);
        }
    }

    private static SSLSession handshakeSession(Socket socket) {
        return socket instanceof SSLSocket sslSocket ? sslSocket.getHandshakeSession() : null;
    }

    private CertificateException reject(Peer peer, String reason, Exception cause) {
        String message = "The TLS configuration '" + tlsConfigurationName + "' rejected the " + peer.description
                + ": " + reason;
        LOG.debug(message);
        return new CertificateException(message, cause);
    }

    private static String trustDomainOf(String spiffeId) {
        // the SPIFFE ID has been validated, the authority is the trust domain
        return URI.create(spiffeId).getHost();
    }
}
