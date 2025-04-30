package io.quarkus.vertx.core.runtime.config;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface AddressResolverConfiguration {

    /**
     * The maximum amount of time in seconds that a successfully resolved address will be cached.
     * <p>
     * If not set explicitly, resolved addresses may be cached forever.
     */
    @WithDefault("2147483647")
    int cacheMaxTimeToLive();

    /**
     * The minimum amount of time in seconds that a successfully resolved address will be cached.
     */
    @WithDefault("0")
    int cacheMinTimeToLive();

    /**
     * The amount of time in seconds that an unsuccessful attempt to resolve an address will be cached.
     */
    @WithDefault("0")
    int cacheNegativeTimeToLive();

    /**
     * The maximum number of queries to be sent during a resolution.
     */
    @WithDefault("4")
    int maxQueries();

    /**
     * The duration after which a DNS query is considered to be failed.
     */
    @WithDefault("5S")
    Duration queryTimeout();

    /**
     * Set the path of an alternate hosts configuration file to use instead of the one provided by the os.
     * <p/>
     * The default value is `null`, so the operating system hosts config (e.g. `/etc/hosts`) is used.
     */
    Optional<String> hostsPath();

    /**
     * Set the hosts configuration refresh period in millis, {@code 0} (default) disables it.
     * <p/>
     * The resolver caches the hosts configuration (configured using `quarkus.vertx.resolver.hosts-path` after it has read it.
     * When the content of this file can change, setting a positive refresh period will load the configuration
     * file again when necessary.
     */
    @WithDefault("0")
    int hostRefreshPeriod();

    /**
     * Set the list of DNS server addresses, an address is the IP of the dns server, followed by an optional
     * colon and a port, e.g {@code 8.8.8.8} or {@code 192.168.0.1:40000}. When the list is empty, the resolver
     * will use the list of the system DNS server addresses from the environment, if that list cannot be retrieved
     * it will use Google's public DNS servers {@code "8.8.8.8"} and {@code "8.8.4.4"}.
     **/
    Optional<List<String>> servers();

    /**
     * Set to true to enable the automatic inclusion in DNS queries of an optional record that hints
     * the remote DNS server about how much data the resolver can read per response.
     */
    @WithDefault("false")
    boolean optResourceEnabled();

    /**
     * Set the DNS queries <i>Recursion Desired</i> flag value.
     */
    @WithDefault("true")
    boolean rdFlag();

    /**
     * Set the lists of DNS search domains.
     * <p/>
     * When the search domain list is null, the effective search domain list will be populated using
     * the system DNS search domains.
     */
    Optional<List<String>> searchDomains();

    /**
     * Set the ndots value used when resolving using search domains, the default value is {@code -1} which
     * determines the value from the OS on Linux or uses the value {@code 1}.
     */
    @WithDefault("-1")
    int ndots();

    /**
     * Set to {@code true} to enable round-robin selection of the dns server to use. It spreads the query load
     * among the servers and avoids all lookup to hit the first server of the list.
     */
    Optional<Boolean> rotateServers();

    /**
     * Set to {@code true} to enable round-robin inet address selection of the ip address to use.
     */
    @WithDefault("false")
    boolean roundRobinInetAddress();
}
