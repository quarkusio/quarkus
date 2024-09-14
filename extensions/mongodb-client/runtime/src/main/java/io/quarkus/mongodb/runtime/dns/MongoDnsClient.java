package io.quarkus.mongodb.runtime.dns;

import static java.lang.String.format;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import com.mongodb.MongoConfigurationException;
import com.mongodb.spi.dns.DnsClient;
import com.mongodb.spi.dns.DnsException;

import io.quarkus.mongodb.runtime.MongodbConfig;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.mutiny.Uni;
import io.vertx.core.dns.DnsClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.dns.SrvRecord;

@RegisterForReflection
public class MongoDnsClient implements DnsClient {
    private static final String BASE_CONFIG_NAME = "quarkus." + MongodbConfig.CONFIG_NAME + ".";

    public static final String DNS_LOOKUP_TIMEOUT = BASE_CONFIG_NAME + MongodbConfig.DNS_LOOKUP_TIMEOUT;
    public static final String NATIVE_DNS_LOOKUP_TIMEOUT = BASE_CONFIG_NAME + MongodbConfig.NATIVE_DNS_LOOKUP_TIMEOUT;

    public static final String DNS_LOG_ACTIVITY = BASE_CONFIG_NAME + MongodbConfig.DNS_LOG_ACTIVITY;
    public static final String NATIVE_DNS_LOG_ACTIVITY = BASE_CONFIG_NAME + MongodbConfig.NATIVE_DNS_LOG_ACTIVITY;

    public static final String DNS_SERVER = BASE_CONFIG_NAME + MongodbConfig.DNS_SERVER_HOST;
    public static final String NATIVE_DNS_SERVER = BASE_CONFIG_NAME + MongodbConfig.NATIVE_DNS_SERVER_HOST;
    public static final String DNS_SERVER_PORT = BASE_CONFIG_NAME + MongodbConfig.DNS_SERVER_PORT;
    public static final String NATIVE_DNS_SERVER_PORT = BASE_CONFIG_NAME + MongodbConfig.NATIVE_DNS_SERVER_PORT;

    private final Config config = ConfigProvider.getConfig();

    private final io.vertx.mutiny.core.dns.DnsClient dnsClient;

    // the static fields are used in order to hold DNS resolution result that has been performed on the main thread
    // at application startup
    // the reason we need this is to ensure that no blocking of event loop threads will occur due to DNS resolution
    private static final Map<String, List<SrvRecord>> SRV_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> TXT_CACHE = new ConcurrentHashMap<>();

    MongoDnsClient(io.vertx.core.Vertx vertx) {
        Vertx mutinyVertx = new io.vertx.mutiny.core.Vertx(vertx);

        boolean activity = config.getOptionalValue(DNS_LOG_ACTIVITY, Boolean.class).orElse(false);

        // If the server is not set, we attempt to read the /etc/resolv.conf. If it does not exist, we use the default
        // configuration.
        String server = config.getOptionalValue(DNS_SERVER, String.class).orElseGet(() -> {
            List<String> list = nameServers();
            if (!list.isEmpty()) {
                return list.get(0);
            }
            return null;
        });
        DnsClientOptions dnsClientOptions = new DnsClientOptions()
                .setLogActivity(activity);
        if (server != null) {
            int port = config.getOptionalValue(DNS_SERVER_PORT, Integer.class)
                    .orElse(53);
            dnsClientOptions
                    .setHost(server)
                    .setPort(port);
        }
        dnsClient = mutinyVertx.createDnsClient(dnsClientOptions);
    }

    private static List<String> nameServers() {
        Path conf = Paths.get("/etc/resolv.conf");
        List<String> nameServers = Collections.emptyList();
        if (Files.exists(conf)) {
            try (Stream<String> lines = Files.lines(conf)) {
                nameServers = lines
                        .filter(line -> line.startsWith("nameserver"))
                        .map(line -> line.split(" ")[1])
                        .collect(Collectors.toList());
            } catch (IOException | ArrayIndexOutOfBoundsException e) {
                Logger.getLogger(MongoDnsClientProvider.class).info("Unable to read the /etc/resolv.conf file", e);
            }
        }
        return nameServers;
    }

    @Override
    public List<String> getResourceRecordData(String name, String type) throws DnsException {
        switch (type) {
            case "SRV":
                return resolveSrvRequest(name);
            case "TXT":
                return resolveTxtRequest(name);
            default:
                throw new IllegalArgumentException("Unknown DNS record type: " + type);
        }
    }

    /*
     * The format of SRV record is
     * priority weight port target.
     * e.g.
     * 0 5 5060 example.com.
     * The priority and weight are ignored, and we just concatenate the host (after removing the ending '.') and port with a
     * ':' in between, as expected by ServerAddress.
     * It's required that the srvHost has at least three parts (e.g. foo.bar.baz) and that all of the resolved hosts have a
     * parent
     * domain equal to the domain of the srvHost.
     */
    private List<String> resolveSrvRequest(final String srvHost) {
        List<String> hosts = new ArrayList<>();
        Duration timeout = config.getOptionalValue(DNS_LOOKUP_TIMEOUT, Duration.class)
                .orElse(Duration.ofSeconds(5));

        try {
            List<SrvRecord> srvRecords;
            if (SRV_CACHE.containsKey(srvHost)) {
                srvRecords = SRV_CACHE.get(srvHost);
            } else {
                srvRecords = Uni.createFrom().<List<SrvRecord>> deferred(
                        new Supplier<>() {
                            @Override
                            public Uni<? extends List<SrvRecord>> get() {
                                return dnsClient.resolveSRV(srvHost);
                            }
                        })
                        .onFailure().retry().withBackOff(Duration.ofSeconds(1)).atMost(3)
                        .invoke(new Consumer<>() {
                            @Override
                            public void accept(List<SrvRecord> srvRecords) {
                                SRV_CACHE.put(srvHost, srvRecords);
                            }
                        })
                        .await().atMost(timeout);
            }

            if (srvRecords.isEmpty()) {
                throw new MongoConfigurationException("No SRV records available for host " + srvHost);
            }
            for (SrvRecord srvRecord : srvRecords) {
                String resolvedHost = srvRecord.target().endsWith(".")
                        ? srvRecord.target().substring(0, srvRecord.target().length() - 1)
                        : srvRecord.target();

                hosts.add(format("%d %d %d %s", srvRecord.priority(), srvRecord.weight(), srvRecord.port(), resolvedHost));
            }
        } catch (Throwable e) {
            throw new MongoConfigurationException("Unable to look up SRV record for host " + srvHost, e);
        }

        return hosts;
    }

    /*
     * A TXT record is just a string
     * We require each to be one or more query parameters for a MongoDB connection string.
     * Here we concatenate TXT records together with a '&' separator as required by connection strings
     */
    public List<String> resolveTxtRequest(final String host) {
        if (TXT_CACHE.containsKey(host)) {
            return TXT_CACHE.get(host);
        }
        try {
            Duration timeout = config.getOptionalValue(DNS_LOOKUP_TIMEOUT, Duration.class)
                    .orElse(Duration.ofSeconds(5));

            return Uni.createFrom().<List<String>> deferred(
                    new Supplier<>() {
                        @Override
                        public Uni<? extends List<String>> get() {
                            return dnsClient.resolveTXT(host);
                        }
                    })
                    .onFailure().retry().withBackOff(Duration.ofSeconds(1)).atMost(3)
                    .invoke(new Consumer<>() {
                        @Override
                        public void accept(List<String> strings) {
                            TXT_CACHE.put(host, strings);
                        }
                    })
                    .await().atMost(timeout);
        } catch (Throwable e) {
            throw new MongoConfigurationException("Unable to look up TXT record for host " + host, e);
        }
    }
}
