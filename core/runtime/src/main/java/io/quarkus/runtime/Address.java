package io.quarkus.runtime;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnixDomainSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.smallrye.common.constraint.Assert;

/**
 * Represents where a service can be reached: the supported protocol and the {@link SocketAddress socket addresses}
 * it is bound to, either ({@link InetSocketAddress}) or Unix domain ({@link UnixDomainSocketAddress})
 * socket addresses.
 * <p>
 * A service publishes its {@link Address} under a name, such as {@code vertx-http} for the HTTP server, once it has
 * bound its socket addresses and they are resolved. The {@link Address} carries the actual values known only at
 * application startup, such as a random port, and not the configured ones. Publication only means that the socket
 * addresses are bound, and not that the service is ready to handle requests.
 * <p>
 * An {@link Address} says nothing about what is served, so it carries no path or other URL components. A consumer that
 * needs a URL builds it from the {@link #protocol()} and one of the {@link #addresses()}.
 * <p>
 * An {@link Address} is available to a service as a dependency, with {@code require(Address.class, "<name>")} or
 * {@code request(Address.class, "<name>")} when the service may not be bound, and to CDI beans as an injection point,
 * with {@code @Named("<name>") Address}. A URL in configuration refers to it as {@code quarkus://<name>/path}, which
 * the {@link AddressResolver} resolves with the {@code @Named} bean.
 */
public final class Address {
    private final String protocol;
    private final List<SocketAddress> addresses;

    private Address(final String protocol, final List<SocketAddress> addresses) {
        Assert.checkNotNullParam("protocol", protocol);
        Assert.checkNotEmptyParam("protocol", protocol);
        Assert.checkNotNullParam("addresses", addresses);
        Assert.checkNotEmptyParam("addresses", addresses);
        for (SocketAddress address : addresses) {
            switch (address) {
                case UnixDomainSocketAddress unix -> {
                }
                case InetSocketAddress inet -> {
                    if (inet.isUnresolved()) {
                        throw new IllegalArgumentException("An address requires resolved socket addresses, but "
                                + inet.getHostString() + ":" + inet.getPort() + " is unresolved");
                    }
                }
                default -> throw new IllegalArgumentException(
                        "Unsupported socket address type " + address.getClass().getName());
            }
        }
        this.protocol = protocol;
        this.addresses = sorted(addresses);
    }

    private static List<SocketAddress> sorted(final List<SocketAddress> addresses) {
        List<SocketAddress> sorted = new ArrayList<>(addresses);
        sorted.sort(Address::compare);
        return List.copyOf(sorted);
    }

    /**
     * Orders socket addresses as described in {@link #addresses()}: Unix domain socket addresses by path, then
     * TCP/IP socket addresses by address and port.
     */
    private static int compare(final SocketAddress a, final SocketAddress b) {
        return switch (a) {
            case UnixDomainSocketAddress ua -> switch (b) {
                case UnixDomainSocketAddress ub -> ua.getPath().compareTo(ub.getPath());
                case InetSocketAddress ib -> -1; // USDA comes before ISA
                default -> throw Assert.impossibleSwitchCase(b);
            };
            case InetSocketAddress ia -> switch (b) {
                case UnixDomainSocketAddress ub -> 1; // UDSA comes before ISA
                case InetSocketAddress ib -> {
                    int byAddress = compare(ia.getAddress(), ib.getAddress());
                    yield byAddress != 0 ? byAddress : Integer.compare(ia.getPort(), ib.getPort());
                }
                default -> throw Assert.impossibleSwitchCase(b);
            };
            default -> throw Assert.impossibleSwitchCase(a);
        };
    }

    /**
     * Orders IP addresses: IPv4 addresses by numeric value, then IPv6 addresses by numeric value and scope.
     */
    private static int compare(final InetAddress a, final InetAddress b) {
        return switch (a) {
            case Inet4Address a4 -> switch (b) {
                case Inet4Address b4 -> compare(a4, b4);
                case Inet6Address b6 -> -1; // IPv4 before IPv6
                default -> throw Assert.impossibleSwitchCase(b);
            };
            case Inet6Address a6 -> switch (b) {
                case Inet4Address b4 -> 1; // IPv4 before IPv6
                case Inet6Address b6 -> compare(a6, b6);
                default -> throw Assert.impossibleSwitchCase(b);
            };
            default -> throw Assert.impossibleSwitchCase(a);
        };
    }

    private static int compare(final Inet4Address a, final Inet4Address b) {
        return Arrays.compareUnsigned(a.getAddress(), b.getAddress());
    }

    private static int compare(final Inet6Address a, final Inet6Address b) {
        int byValue = Arrays.compareUnsigned(a.getAddress(), b.getAddress());
        return byValue != 0 ? byValue : Integer.compare(a.getScopeId(), b.getScopeId());
    }

    /**
     * Creates an {@link Address} for a service speaking the specified protocol on the specified socket addresses.
     *
     * @param protocol the protocol the service speaks, such as {@code http}, {@code https} or {@code grpc}
     * @param addresses the socket addresses the service is bound to
     * @return a new {@link Address}
     * @throws IllegalArgumentException if the specified protocol is empty, no socket address is specified, or a
     *         socket address is unresolved or of an unsupported type
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
     * @throws IllegalArgumentException if the specified protocol is empty, the specified list is empty, or a socket
     *         address is unresolved or of an unsupported type
     */
    public static Address of(final String protocol, final List<SocketAddress> addresses) {
        return new Address(protocol, addresses);
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
     * path, then IPv4 socket addresses by numeric value, then IPv6 socket addresses by numeric value and scope, each
     * followed by port.
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
        return this == o || o instanceof Address a && protocol.equals(a.protocol) && addresses.equals(a.addresses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocol, addresses);
    }

    @Override
    public String toString() {
        return "Address[protocol=" + protocol + ", addresses=" + addresses + "]";
    }
}
