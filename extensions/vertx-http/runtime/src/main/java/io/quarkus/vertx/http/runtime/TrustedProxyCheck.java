package io.quarkus.vertx.http.runtime;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;

import io.vertx.core.net.SocketAddress;

public interface TrustedProxyCheck {

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
