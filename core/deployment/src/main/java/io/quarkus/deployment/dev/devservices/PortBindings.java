package io.quarkus.deployment.dev.devservices;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Helpers to select the host port binding of a container port.
 * <p>
 * On dual-stack hosts (e.g. Docker Desktop on macOS), Docker may publish the IPv4 and IPv6 bindings of the same
 * container port on <em>different</em> host ports. Picking an arbitrary binding can therefore return the IPv6 host port
 * while the application connects over IPv4. These helpers always prefer the IPv4 binding and fall back to IPv6 only
 * when there is no IPv4 binding.
 */
public final class PortBindings {

    private PortBindings() {
    }

    /**
     * @param hostIp the host IP of a port binding, may be {@code null}
     * @return {@code true} if the given host IP is an IPv6 address (IPv6 addresses always contain a colon, IPv4 never do)
     */
    public static boolean isIpv6(String hostIp) {
        return hostIp != null && hostIp.contains(":");
    }

    /**
     * Selects the binding of the given container (private) port, preferring IPv4.
     * Only bindings with a public (host) port are considered.
     *
     * @param ports the container port bindings, may be {@code null}
     * @param privatePort the container (private) port
     * @return the selected binding
     */
    public static Optional<ContainerInfo.ContainerPort> selectBinding(ContainerInfo.ContainerPort[] ports,
            int privatePort) {
        return selectBinding(ports,
                p -> p.publicPort() != null && p.privatePort() != null && p.privatePort() == privatePort,
                ContainerInfo.ContainerPort::ip);
    }

    /**
     * Selects one binding per container (private) port, preferring IPv4.
     * Only bindings with both a private and a public (host) port are considered.
     *
     * @param ports the container port bindings, may be {@code null}
     * @return the selected bindings, in encounter order of the private ports
     */
    public static Collection<ContainerInfo.ContainerPort> selectBindings(ContainerInfo.ContainerPort[] ports) {
        return selectBindings(ports,
                p -> p.publicPort() != null && p.privatePort() != null,
                ContainerInfo.ContainerPort::privatePort,
                ContainerInfo.ContainerPort::ip);
    }

    /**
     * Selects the first binding matching the filter, preferring IPv4.
     *
     * @param bindings the port bindings, may be {@code null}
     * @param filter the bindings to consider
     * @param hostIp extracts the host IP of a binding
     * @return the first matching IPv4 binding, or the first matching IPv6 binding if there is no IPv4 one
     */
    public static <T> Optional<T> selectBinding(T[] bindings, Predicate<T> filter, Function<T, String> hostIp) {
        if (bindings == null) {
            return Optional.empty();
        }
        T ipv6Binding = null;
        for (T binding : bindings) {
            if (binding == null || !filter.test(binding)) {
                continue;
            }
            if (!isIpv6(hostIp.apply(binding))) {
                return Optional.of(binding);
            }
            if (ipv6Binding == null) {
                ipv6Binding = binding;
            }
        }
        return Optional.ofNullable(ipv6Binding);
    }

    /**
     * Selects one binding per container (private) port among the bindings matching the filter, preferring IPv4.
     *
     * @param bindings the port bindings, may be {@code null}
     * @param filter the bindings to consider
     * @param privatePort extracts the container (private) port of a binding
     * @param hostIp extracts the host IP of a binding
     * @return the selected bindings, in encounter order of the private ports
     */
    public static <T> Collection<T> selectBindings(T[] bindings, Predicate<T> filter, Function<T, Integer> privatePort,
            Function<T, String> hostIp) {
        Map<Integer, T> selected = new LinkedHashMap<>();
        if (bindings == null) {
            return selected.values();
        }
        for (T binding : bindings) {
            if (binding == null || !filter.test(binding)) {
                continue;
            }
            Integer port = Objects.requireNonNull(privatePort.apply(binding), "privatePort");
            T current = selected.get(port);
            if (current == null || (isIpv6(hostIp.apply(current)) && !isIpv6(hostIp.apply(binding)))) {
                selected.put(port, binding);
            }
        }
        return selected.values();
    }
}
