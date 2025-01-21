package io.quarkus.vertx.http.runtime;

import static io.quarkus.vertx.http.runtime.TrustedProxyCheck.denyAll;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.smallrye.common.net.Inet;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.dns.DnsClient;
import io.vertx.core.http.HttpServerRequest;
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
            dnsClient.lookup(hostName,
                    new Handler<AsyncResult<String>>() {
                        @Override
                        public void handle(AsyncResult<String> stringAsyncResult) {
                            if (stringAsyncResult.succeeded() && stringAsyncResult.result() != null) {
                                var trustedIP = Inet.parseInetAddress(stringAsyncResult.result());
                                if (trustedIP != null) {
                                    // create proxy check for resolved IP and proceed with the lookup
                                    lookupHostNamesAndHandleRequest(event, iterator,
                                            builder.withTrustedIP(trustedIP, entry.getValue()), dnsClient);
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
            vertx.get().createDnsClient().lookup(hostName,
                    new Handler<AsyncResult<String>>() {
                        @Override
                        public void handle(AsyncResult<String> stringAsyncResult) {
                            TrustedProxyCheck proxyCheck;
                            if (stringAsyncResult.succeeded()) {
                                // use resolved IP to build proxy check
                                final var proxyIP = Inet.parseInetAddress(stringAsyncResult.result());
                                if (proxyIP != null) {
                                    proxyCheck = builder.build(proxyIP, event.remoteAddress().port());
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
                        }
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
