package io.quarkus.runtime;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnixDomainSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.quarkus.value.registry.ValueRegistry;
import io.quarkus.value.registry.ValueRegistry.RuntimeKey;

/**
 * Represents where a service can be reached: the supported protocol and the {@link SocketAddress socket addresses}
 * it is bound to, either ({@link InetSocketAddress}) or Unix domain ({@link UnixDomainSocketAddress})
 * socket addresses.
 * <p>
 * A service registers its {@link Address} in the {@link ValueRegistry} under a name, such as {@code vertx-http} for
 * the HTTP server, once it has bound its socket addresses. The {@link Address} carries the actual values known only
 * at application startup, such as a random port, and not the configured ones. Registration only means that the socket
 * addresses are bound, and not that the service is ready to handle requests.
 * <p>
 * An {@link Address} says nothing about what is served, so it carries no path or other URL components. A consumer that
 * needs a URL builds it from the {@link #protocol()} and one of the {@link #addresses()}.
 * <p>
 * An {@link Address} is available to a service as a dependency, with {@code require(Address.class, "<name>")} or
 * {@code request(Address.class, "<name>")} when the service may not be bound, and to CDI beans as an injection point,
 * with {@code @Named("<name>") Address}. Both are backed by the {@link ValueRegistry}, where the {@link Address} is
 * registered under {@link #key(String)}.
 */
public final class Address {
    private static final String KEY_PREFIX = "quarkus.address.";

    private final String protocol;
    private final List<SocketAddress> addresses;

    private Address(final String protocol, final List<SocketAddress> addresses) {
        if (protocol == null || protocol.isBlank()) {
            throw new IllegalArgumentException("An address requires a protocol");
        }
        if (addresses.isEmpty()) {
            throw new IllegalArgumentException("An address requires at least one socket address");
        }
        this.protocol = protocol;
        this.addresses = sorted(addresses);
    }

    private static List<SocketAddress> sorted(final List<SocketAddress> addresses) {
        List<SocketAddress> sorted = new ArrayList<>(addresses);
        sorted.sort(SocketOrder.INSTANCE);
        return List.copyOf(sorted);
    }

    /**
     * Creates an {@link Address} for a service speaking the specified protocol on the specified socket addresses.
     *
     * @param protocol the protocol the service speaks, such as {@code http}, {@code https} or {@code grpc}
     * @param addresses the socket addresses the service is bound to
     * @return a new {@link Address}
     * @throws IllegalArgumentException if the specified protocol is blank or no socket address is specified
     */
    public static Address of(final String protocol, final SocketAddress... addresses) {
        return new Address(protocol, List.of(addresses));
    }

    /**
     * Creates an {@link Address} for a service speaking the specified protocol on the specified socket addresses.
     *
     * @param protocol the protocol the service speaks, such as {@code http}, {@code https} or {@code grpc}
     * @param addresses the socket addresses the service is bound to
     * @return a new {@link Address}
     * @throws IllegalArgumentException if the specified protocol is blank or the specified list is empty
     */
    public static Address of(final String protocol, final List<SocketAddress> addresses) {
        return new Address(protocol, addresses);
    }

    /**
     * Returns the {@link RuntimeKey} with which a service registers its {@link Address} in the {@link ValueRegistry}.
     *
     * @param name the name of the {@link Address}, such as {@code vertx-http}
     * @return the key with which the {@link Address} with the specified name is registered
     */
    public static RuntimeKey<Address> key(final String name) {
        return RuntimeKey.key(KEY_PREFIX + name, Address.class);
    }

    /**
     * Returns the protocol the service speaks.
     *
     * @return the protocol, such as {@code http}, {@code https} or {@code grpc}
     */
    public String protocol() {
        return protocol;
    }

    /**
     * Returns the socket addresses the service is bound to, in a canonical order: Unix domain socket addresses by
     * path, then IPv4 socket addresses by numeric value, then IPv6 socket addresses by numeric value and scope, and
     * then unresolved socket addresses by host name, each followed by port.
     *
     * @return an unmodifiable list with at least one socket address
     */
    public List<SocketAddress> addresses() {
        return addresses;
    }

    /**
     * Returns the first TCP/IP socket address the service is bound to, following the order of {@link #addresses()}.
     *
     * @return the first TCP/IP socket address, or an empty {@link Optional} if the service is bound to none
     */
    public Optional<InetSocketAddress> inet() {
        return first(InetSocketAddress.class);
    }

    /**
     * Returns the first Unix domain socket address the service is bound to, following the order of
     * {@link #addresses()}.
     *
     * @return the first Unix domain socket address, or an empty {@link Optional} if the service is bound to none
     */
    public Optional<UnixDomainSocketAddress> unix() {
        return first(UnixDomainSocketAddress.class);
    }

    private <T extends SocketAddress> Optional<T> first(final Class<T> type) {
        for (SocketAddress address : addresses) {
            if (type.isInstance(address)) {
                return Optional.of(type.cast(address));
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Address address)) {
            return false;
        }
        return protocol.equals(address.protocol) && addresses.equals(address.addresses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocol, addresses);
    }

    @Override
    public String toString() {
        return "Address[protocol=" + protocol + ", addresses=" + addresses + "]";
    }

    /**
     * Orders socket addresses as described in {@link Address#addresses()}.
     */
    private enum SocketOrder implements Comparator<SocketAddress> {
        INSTANCE;

        @Override
        public int compare(final SocketAddress a, final SocketAddress b) {
            int byKind = Integer.compare(kind(a), kind(b));
            if (byKind != 0) {
                return byKind;
            }
            if (a instanceof UnixDomainSocketAddress ua && b instanceof UnixDomainSocketAddress ub) {
                return ua.getPath().compareTo(ub.getPath());
            }
            if (a instanceof InetSocketAddress ia && b instanceof InetSocketAddress ib) {
                int byHost = ia.isUnresolved()
                        ? ia.getHostString().compareTo(ib.getHostString())
                        : compare(ia.getAddress(), ib.getAddress());
                return byHost != 0 ? byHost : Integer.compare(ia.getPort(), ib.getPort());
            }
            return a.toString().compareTo(b.toString());
        }

        private static int compare(final InetAddress a, final InetAddress b) {
            int byValue = Arrays.compareUnsigned(a.getAddress(), b.getAddress());
            if (byValue != 0) {
                return byValue;
            }
            if (a instanceof Inet6Address a6 && b instanceof Inet6Address b6) {
                return Integer.compare(a6.getScopeId(), b6.getScopeId());
            }
            return 0;
        }

        /**
         * Returns the rank of the kind of the specified socket address: Unix domain, then IPv4, then IPv6, then
         * unresolved.
         */
        private static int kind(final SocketAddress address) {
            if (address instanceof UnixDomainSocketAddress) {
                return 0;
            }
            if (address instanceof InetSocketAddress inet) {
                if (inet.isUnresolved()) {
                    return 3;
                }
                return inet.getAddress() instanceof Inet4Address ? 1 : 2;
            }
            return 4;
        }
    }
}
