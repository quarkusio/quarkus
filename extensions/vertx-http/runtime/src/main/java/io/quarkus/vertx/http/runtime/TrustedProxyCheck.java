package io.quarkus.vertx.http.runtime;

import java.net.InetAddress;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.auth.x500.X500Principal;

import org.jboss.logging.Logger;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;

public interface TrustedProxyCheck {

    Logger LOGGER = Logger.getLogger(TrustedProxyCheck.class.getName());

    static TrustedProxyCheck allowAll() {
        return new TrustedProxyCheck() {
            @Override
            public boolean isProxyAllowed() {
                return true;
            }
        };
    }

    static TrustedProxyCheck denyAll() {
        return new TrustedProxyCheck() {
            @Override
            public boolean isProxyAllowed() {
                return false;
            }
        };
    }

    /**
     * User can configure trusted proxies for `Forwarded`, `X-Forwarded` or `X-Forwarded-*` headers.
     * Headers from untrusted proxies must be ignored.
     *
     * @return true if `Forwarded`, `X-Forwarded` or `X-Forwarded-*` headers were sent by trusted {@link SocketAddress}
     */
    boolean isProxyAllowed();

    static TrustedProxyCheck createTrustedProxyDnCheck(HttpServerRequest event, List<List<Rdn>> trustedDns) {
        final SSLSession sslSession = event.sslSession();
        if (sslSession == null) {
            LOGGER.debug("No SSL session, proxy DN check cannot be performed");
            return denyAll();
        }

        final Certificate[] peerCertificates;
        try {
            peerCertificates = sslSession.getPeerCertificates();
        } catch (SSLPeerUnverifiedException e) {
            LOGGER.debug("Peer certificate not available, proxy DN check cannot be performed");
            return denyAll();
        }

        if (peerCertificates == null || peerCertificates.length == 0
                || !(peerCertificates[0] instanceof X509Certificate peerCert)) {
            LOGGER.debug("No X509 peer certificate, proxy DN check cannot be performed");
            return denyAll();
        }

        final X500Principal peerDn = peerCert.getSubjectX500Principal();
        if (matchesAnyTrustedDn(peerDn, trustedDns)) {
            LOGGER.debugf("Proxy DN '%s' matches trusted DN", peerDn);
            return allowAll();
        }

        LOGGER.debugf("Proxy DN '%s' does not match any trusted DN", peerDn);
        return denyAll();
    }

    static boolean matchesAnyTrustedDn(X500Principal peerDn, List<List<Rdn>> trustedDns) {
        final Set<Rdn> peerRdns;
        try {
            peerRdns = new HashSet<>(new LdapName(peerDn.getName()).getRdns());
        } catch (InvalidNameException e) {
            return false;
        }
        for (List<Rdn> requiredRdns : trustedDns) {
            if (peerRdns.containsAll(requiredRdns)) {
                return true;
            }
        }
        return false;
    }

    final class TrustedProxyCheckBuilder {

        private final Map<String, Integer> hostNameToPort;
        private final List<BiPredicate<InetAddress, Integer>> proxyChecks;

        private TrustedProxyCheckBuilder(Map<String, Integer> hostNameToPort,
                List<BiPredicate<InetAddress, Integer>> proxyChecks) {
            this.hostNameToPort = hasHostNames(hostNameToPort) ? Map.copyOf(hostNameToPort) : null;
            this.proxyChecks = List.copyOf(proxyChecks);
        }

        static TrustedProxyCheckBuilder builder(List<TrustedProxyCheckPart> parts) {
            final Map<String, Integer> hostNameToPort = new HashMap<>();
            final List<BiPredicate<InetAddress, Integer>> proxyChecks = new ArrayList<>();
            for (TrustedProxyCheckPart part : parts) {
                if (part.proxyCheck != null) {
                    proxyChecks.add(part.proxyCheck);
                } else {
                    hostNameToPort.put(part.hostName, part.port);
                }
            }
            return new TrustedProxyCheckBuilder(hostNameToPort, proxyChecks);
        }

        TrustedProxyCheckBuilder withTrustedIP(Collection<InetAddress> trustedIP, int trustedPort) {
            final List<BiPredicate<InetAddress, Integer>> proxyChecks = new ArrayList<>(this.proxyChecks);
            proxyChecks.add(createNewIpCheck(trustedIP, trustedPort));
            return new TrustedProxyCheckBuilder(null, proxyChecks);
        }

        boolean hasProxyChecks() {
            return !proxyChecks.isEmpty();
        }

        TrustedProxyCheck build(InetAddress proxyIP, int proxyPort) {
            Objects.requireNonNull(proxyIP);
            return new TrustedProxyCheck() {
                @Override
                public boolean isProxyAllowed() {
                    for (BiPredicate<InetAddress, Integer> proxyCheck : proxyChecks) {
                        if (proxyCheck.test(proxyIP, proxyPort)) {
                            return true;
                        }
                    }
                    return false;
                }
            };
        }

        TrustedProxyCheck build(Collection<InetAddress> proxyIPs, int proxyPort) {
            Objects.requireNonNull(proxyIPs);
            return () -> {
                for (BiPredicate<InetAddress, Integer> proxyCheck : proxyChecks) {
                    for (InetAddress proxyIP : proxyIPs) {
                        if (proxyCheck.test(proxyIP, proxyPort)) {
                            return true;
                        }
                    }
                }
                return false;
            };
        }

        boolean hasHostNames() {
            return hasHostNames(this.hostNameToPort);
        }

        private static boolean hasHostNames(Map<String, Integer> hostNameToPort) {
            return hostNameToPort != null && !hostNameToPort.isEmpty();
        }

        public Map<String, Integer> getHostNameToPort() {
            return hostNameToPort;
        }

    }

    static BiPredicate<InetAddress, Integer> createNewIpCheck(InetAddress trustedIP, int trustedPort) {
        final boolean doNotCheckPort = trustedPort == 0;
        return new BiPredicate<>() {
            @Override
            public boolean test(InetAddress proxyIP, Integer proxyPort) {
                return isPortOk(proxyPort) && trustedIP.equals(proxyIP);
            }

            private boolean isPortOk(int port) {
                return doNotCheckPort || port == trustedPort;
            }
        };
    }

    static BiPredicate<InetAddress, Integer> createNewIpCheck(Collection<InetAddress> trustedIP, int trustedPort) {
        final boolean doNotCheckPort = trustedPort == 0;
        return new BiPredicate<>() {
            @Override
            public boolean test(InetAddress proxyIP, Integer proxyPort) {
                return isPortOk(proxyPort) && trustedIP.contains(proxyIP);
            }

            private boolean isPortOk(int port) {
                return doNotCheckPort || port == trustedPort;
            }
        };
    }

    final class TrustedProxyCheckPart {

        final BiPredicate<InetAddress, Integer> proxyCheck;
        final String hostName;
        final int port;

        TrustedProxyCheckPart(BiPredicate<InetAddress, Integer> proxyCheck) {
            this.proxyCheck = proxyCheck;
            this.hostName = null;
            this.port = 0;
        }

        TrustedProxyCheckPart(String hostName, int port) {
            this.proxyCheck = null;
            this.hostName = hostName;
            this.port = port;
        }

    }

}
