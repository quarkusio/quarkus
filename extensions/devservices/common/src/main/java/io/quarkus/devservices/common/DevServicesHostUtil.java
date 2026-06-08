package io.quarkus.devservices.common;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;
import org.testcontainers.DockerClientFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.ContainerNetwork;

/**
 * Host resolution and URI authority formatting for Dev Services containers.
 */
public final class DevServicesHostUtil {

    private static final Logger LOG = Logger.getLogger(DevServicesHostUtil.class);

    private static final String DOCKER_BRIDGE_NETWORK = "bridge";

    private static final String PODMAN_DEFAULT_NETWORK = "podman";

    private static final Map<String, String> RESOLVED_HOST_CACHE = new ConcurrentHashMap<>();

    private static final String HOST_OVERRIDE = resolveHostOverride();

    private DevServicesHostUtil() {
    }

    public static String formatHostForUriAuthority(String host) {
        Objects.requireNonNull(host, "host");
        if (host.isEmpty() || host.charAt(0) == '[' || !isIPv6Literal(host)) {
            return host;
        }
        return "[" + host + "]";
    }

    public static String formatHostAndPort(String host, int port) {
        return formatHostForUriAuthority(host) + ":" + port;
    }

    public static String formatPrefixedAuthority(String prefix, String host, int port) {
        return prefix + "://" + formatHostAndPort(host, port);
    }

    /**
     * Returns the host to use for connecting to a published port, resolving IPv6 gateway
     * addresses to a reachable IPv4 alternative or {@code localhost} where needed.
     */
    public static String resolvePublishedPortHost(String containerId, String reportedHost) {
        Objects.requireNonNull(reportedHost, "reportedHost");

        // Normalise special "bind-all" address — always means localhost from host side.
        if (reportedHost.isEmpty() || "0.0.0.0".equals(reportedHost) || "::".equals(reportedHost)) {
            return "localhost";
        }

        // Explicit user override takes priority (e.g. Colima, custom Podman machine).
        if (HOST_OVERRIDE != null) {
            return HOST_OVERRIDE;
        }

        String bare = unbracket(reportedHost);

        // Not an IPv6 address — nothing to resolve.
        if (!isIPv6Literal(bare)) {
            return bare;
        }

        // IPv6 reported. Attempt to resolve to an IPv4 gateway reachable from the host.
        if (containerId == null || containerId.isBlank()) {
            LOG.infof("Dev Services: no container id available; using localhost instead of IPv6 host %s", bare);
            return "localhost";
        }

        String resolved = RESOLVED_HOST_CACHE.computeIfAbsent(containerId,
                id -> resolveIPv6ToReachableHost(id, bare));
        return ensureReachableFromHost(resolved, bare);
    }

    /**
     * Published ports on the host are never reachable via Docker's IPv6 userland-proxy address
     * (e.g. {@code fd00:d0ca:1::1}). If resolution still yields an IPv6 literal, use localhost.
     */
    private static String ensureReachableFromHost(String resolved, String reportedIPv6) {
        if (!isIPv6Literal(unbracket(resolved))) {
            return resolved;
        }
        LOG.infof("Dev Services: IPv6 host %s is not reachable from the host JVM; using localhost instead of %s",
                reportedIPv6, unbracket(resolved));
        return "localhost";
    }

    public static String publishedPortHost(String containerId, String reportedHost) {
        return resolvePublishedPortHost(containerId, reportedHost);
    }

    public static String publishedPortHost(String containerId, boolean useSharedNetwork,
            String sharedNetworkHost, String reportedHost) {
        if (useSharedNetwork) {
            Objects.requireNonNull(sharedNetworkHost, "sharedNetworkHost");
            return sharedNetworkHost;
        }
        return resolvePublishedPortHost(containerId, reportedHost);
    }

    public static String formatResolvedHostAndPort(String containerId, String reportedHost, int port) {
        return formatHostAndPort(resolvePublishedPortHost(containerId, reportedHost), port);
    }

    public static String formatResolvedPrefixedAuthority(String prefix, String containerId,
            String reportedHost, int port) {
        return prefix + "://" + formatResolvedHostAndPort(containerId, reportedHost, port);
    }

    public static boolean isIPv6Literal(String host) {
        if (host == null || host.isEmpty()) {
            return false;
        }
        if (host.charAt(0) == '[') {
            return true;
        }
        int colons = 0;
        for (int i = 0; i < host.length(); i++) {
            if (host.charAt(i) == ':') {
                if (++colons >= 2) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String resolveHostOverride() {
        String v = System.getenv("TESTCONTAINERS_HOST_OVERRIDE");
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private static String unbracket(String host) {
        if (host.length() >= 2 && host.charAt(0) == '[' && host.charAt(host.length() - 1) == ']') {
            return host.substring(1, host.length() - 1);
        }
        return host;
    }

    private static String resolveIPv6ToReachableHost(String containerId, String ipv6Fallback) {
        try {
            DockerClient client = DockerClientFactory.lazyClient();
            Map<String, ContainerNetwork> networks = client
                    .inspectContainerCmd(containerId)
                    .exec()
                    .getNetworkSettings()
                    .getNetworks();

            if (networks == null || networks.isEmpty()) {
                // Rootless Podman (pasta / slirp4netns): inspect returns no network info.
                // Podman Desktop (Mac/Win): same — the VM port-forwards to localhost.
                LOG.infof(
                        "Dev Services: container %s has no network entries in inspect response "
                                + "(rootless Podman / Podman Desktop?); using localhost instead of IPv6 %s",
                        shortId(containerId), ipv6Fallback);
                return "localhost";
            }

            // Docker bridge network (Docker Engine, Docker Desktop, Rancher Desktop dockerd).
            String gateway = ipv4Gateway(networks.get(DOCKER_BRIDGE_NETWORK));
            if (gateway != null) {
                logResolved(containerId, ipv6Fallback, gateway, DOCKER_BRIDGE_NETWORK);
                return gateway;
            }

            // Podman default network (rootful Podman with Netavark / CNI).
            gateway = ipv4Gateway(networks.get(PODMAN_DEFAULT_NETWORK));
            if (gateway != null) {
                logResolved(containerId, ipv6Fallback, gateway, PODMAN_DEFAULT_NETWORK);
                return gateway;
            }

            // Any other network (Rancher Desktop containerd/CNI, custom named networks).
            for (Map.Entry<String, ContainerNetwork> entry : networks.entrySet()) {
                gateway = ipv4Gateway(entry.getValue());
                if (gateway != null) {
                    logResolved(containerId, ipv6Fallback, gateway, entry.getKey());
                    return gateway;
                }
            }

            // Networks present but all have empty / IPv6-only gateways.
            // This happens with rootless Podman using a named bridge network where the
            // gateway field is populated with an IPv6 address only, or is blank.
            // Published ports are still reachable via localhost.
            LOG.infof(
                    "Dev Services: container %s inspect returned %d network(s) but none had a usable "
                            + "IPv4 gateway; using localhost instead of IPv6 %s",
                    shortId(containerId), networks.size(), ipv6Fallback);
            return "localhost";

        } catch (Exception e) {
            LOG.infof(e,
                    "Dev Services: docker inspect failed for container %s; using localhost instead of IPv6 %s",
                    shortId(containerId), ipv6Fallback);
            return "localhost";
        }
    }

    private static String ipv4Gateway(ContainerNetwork network) {
        if (network == null) {
            return null;
        }
        String gw = network.getGateway();
        if (gw == null || gw.isBlank() || isIPv6Literal(gw)) {
            return null;
        }
        // Extra guard: Podman sometimes returns "0.0.0.0" as a placeholder.
        if ("0.0.0.0".equals(gw)) {
            return null;
        }
        return gw;
    }

    private static void logResolved(String containerId, String ipv6, String ipv4, String networkName) {
        LOG.infof(
                "Dev Services: container %s IPv6 gateway %s resolved to IPv4 gateway %s "
                        + "via network '%s'",
                shortId(containerId), ipv6, ipv4, networkName);
    }

    private static String shortId(String containerId) {
        return containerId.length() > 12 ? containerId.substring(0, 12) : containerId;
    }
}
