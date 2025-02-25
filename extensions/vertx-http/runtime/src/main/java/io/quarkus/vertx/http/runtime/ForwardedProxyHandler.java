package io.quarkus.vertx.http.runtime;

import static io.quarkus.vertx.http.runtime.TrustedProxyCheck.denyAll;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.smallrye.common.net.Inet;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.dns.DnsClient;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.impl.SocketAddressImpl;

/**
 * Restricts who can send `Forwarded`, `X-Forwarded` or `X-Forwarded-*` headers to trusted proxies
 * configured through {@link ProxyConfig#trustedProxies}.
 */
public class ForwardedProxyHandler implements Handler<HttpServerRequest> {

    private static final Logger LOGGER = Logger.getLogger(ForwardedProxyHandler.class.getName());

    private final TrustedProxyCheck.TrustedProxyCheckBuilder proxyCheckBuilder;

    private final Supplier<Vertx> vertx;

    private final Handler<HttpServerRequest> delegate;

    private final ForwardingProxyOptions forwardingProxyOptions;

    public ForwardedProxyHandler(TrustedProxyCheck.TrustedProxyCheckBuilder proxyCheckBuilder,
            Supplier<Vertx> vertx, Handler<HttpServerRequest> delegate,
            ForwardingProxyOptions forwardingProxyOptions) {
        this.proxyCheckBuilder = proxyCheckBuilder;
        this.vertx = vertx;
        this.delegate = delegate;
        this.forwardingProxyOptions = forwardingProxyOptions;
    }

    @Override
    public void handle(HttpServerRequest event) {
        if (event.remoteAddress() == null) {
            // client address may not be available with virtual http channel
            LOGGER.debug("Client address is not available, 'Forwarded' and 'X-Forwarded' headers are going to be ignored");
            handleForwardedServerRequest(event, denyAll());
        } else if (event.remoteAddress().isDomainSocket()) {
            // we do not support domain socket proxy checks, ignore the headers
            LOGGER.debug("Domain socket are not supported, 'Forwarded' and 'X-Forwarded' headers are going to be ignored");
            handleForwardedServerRequest(event, denyAll());
        } else {
            // create proxy check, then handle request
            if (proxyCheckBuilder.hasHostNames()) {
                // we need to perform DNS lookup for trusted proxy hostnames
                lookupHostNamesAndHandleRequest(event,
                        proxyCheckBuilder.getHostNameToPort().entrySet().iterator(), proxyCheckBuilder,
                        vertx.get().createDnsClient());
            } else {
                resolveProxyIpAndHandleRequest(event, proxyCheckBuilder);
            }
        }
    }

    private void lookupHostNamesAndHandleRequest(HttpServerRequest event,
            Iterator<Map.Entry<String, Integer>> iterator,
            TrustedProxyCheck.TrustedProxyCheckBuilder builder,
            DnsClient dnsClient) {
        if (iterator.hasNext()) {
            // perform recursive DNS lookup for all hostnames
            // we do not cache result as IP address may change, and we advise users to use IP or CIDR
            final var entry = iterator.next();
            final String hostName = entry.getKey();

            resolveHostNameToAllIpAddresses(dnsClient, hostName, event.remoteAddress(), results -> {
                if (!results.isEmpty()) {
                    Set<InetAddress> trustedIPs = results.stream().map(Inet::parseInetAddress).filter(Objects::nonNull)
                            .collect(Collectors.toSet());
                    if (!trustedIPs.isEmpty()) {
                        // create proxy check for resolved IP and proceed with the lookup
                        lookupHostNamesAndHandleRequest(event, iterator,
                                builder.withTrustedIP(trustedIPs, entry.getValue()), dnsClient);
                    } else {
                        logInvalidIpAddress(hostName);
                        // ignore this hostname proxy check and proceed with the lookup
                        lookupHostNamesAndHandleRequest(event, iterator, builder, dnsClient);
                    }
                } else {
                    // inform we can't cope without IP
                    logDnsLookupFailure(hostName);
                    // ignore this hostname proxy check and proceed with the lookup
                    lookupHostNamesAndHandleRequest(event, iterator, builder, dnsClient);
                }
            });

        } else {
            // DNS lookup is done
            if (builder.hasProxyChecks()) {
                resolveProxyIpAndHandleRequest(event, builder);
            } else {
                // ignore headers as there are no proxy checks
                handleForwardedServerRequest(event, denyAll());
            }
        }
    }

    private void resolveHostNameToAllIpAddresses(DnsClient dnsClient, String hostName, SocketAddress callersSocketAddress,
            Handler<Collection<String>> handler) {
        ArrayList<Future<List<String>>> results = new ArrayList<>();
        InetAddress proxyIP = null;
        if (callersSocketAddress != null) {
            proxyIP = ((SocketAddressImpl) callersSocketAddress).ipAddress();
        }
        // Match the lookup with the address type of the caller
        if (proxyIP == null || proxyIP instanceof Inet4Address) {
            results.add(dnsClient.resolveA(hostName));
        }
        if (proxyIP == null || proxyIP instanceof Inet6Address) {
            results.add(dnsClient.resolveAAAA(hostName));
        }
        processFutures(results, new ArrayList<>(), handler);
    }

    private void processFutures(ArrayList<Future<List<String>>> future, Collection<String> results,
            Handler<Collection<String>> handler) {
        if (!future.isEmpty()) {
            Future<List<String>> poll = future.remove(0);
            poll.onComplete(result -> {
                if (result.succeeded() && result.result() != null) {
                    results.addAll(result.result());
                }
                processFutures(future, results, handler);
            });
        } else {
            handler.handle(results);
        }
    }

    private void resolveProxyIpAndHandleRequest(HttpServerRequest event,
            TrustedProxyCheck.TrustedProxyCheckBuilder builder) {
        InetAddress proxyIP = ((SocketAddressImpl) event.remoteAddress()).ipAddress();
        if (proxyIP == null) {
            // if host is an IP address, proxyIP won't be null
            proxyIP = Inet.parseInetAddress(event.remoteAddress().host());
        }

        if (proxyIP == null) {
            // perform DNS lookup, then create proxy check and handle request
            final String hostName = Objects.requireNonNull(event.remoteAddress().hostName());
            resolveHostNameToAllIpAddresses(vertx.get().createDnsClient(), hostName, null,
                    results -> {
                        TrustedProxyCheck proxyCheck;
                        if (!results.isEmpty()) {
                            // use resolved IP to build proxy check
                            Set<InetAddress> proxyIPs = results.stream().map(Inet::parseInetAddress).filter(Objects::nonNull)
                                    .collect(Collectors.toSet());
                            if (!proxyIPs.isEmpty()) {
                                proxyCheck = builder.build(proxyIPs, event.remoteAddress().port());
                            } else {
                                logInvalidIpAddress(hostName);
                                proxyCheck = denyAll();
                            }
                        } else {
                            // we can't cope without IP => ignore headers
                            logDnsLookupFailure(hostName);
                            proxyCheck = denyAll();
                        }

                        handleForwardedServerRequest(event, proxyCheck);
                    });
        } else {
            // we have proxy IP => create proxy check and handle request
            var proxyCheck = builder.build(proxyIP, event.remoteAddress().port());
            handleForwardedServerRequest(event, proxyCheck);
        }
    }

    private void handleForwardedServerRequest(HttpServerRequest event, TrustedProxyCheck proxyCheck) {
        delegate.handle(new ForwardedServerRequestWrapper(event, forwardingProxyOptions, proxyCheck));
    }

    private static void logInvalidIpAddress(String hostName) {
        LOGGER.debugf("Illegal state - DNS server returned invalid IP address for hostname '%s'", hostName);
    }

    private static void logDnsLookupFailure(String hostName) {
        LOGGER.debugf("Can't resolve proxy IP address from '%s'", hostName);
    }
}
