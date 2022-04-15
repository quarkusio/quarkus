package io.quarkus.mongodb.runtime.dns;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.vertx.core.dns.DnsClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.dns.DnsClient;

/**
 * This class is used in native mode when {@code quarkus.mongodb.native.dns.use-vertx-dns-resolver} is set to {@code true}.
 * By default, the MongoDB driver uses JNDI to retrieve the SRV and TXT DNS Records, which is not supported in native.
 */
public abstract class DnsClientProducer {

    public static final String FLAG = "quarkus.mongodb.native.dns.use-vertx-dns-resolver";

    private static final String DNS_SERVER = "quarkus.mongodb.native.dns.server-host";
    private static final String DNS_SERVER_PORT = "quarkus.mongodb.native.dns.server-port";
    private static final String DNS_SERVER_ACTIVITY = "quarkus.mongodb.native.dns.log-activity";

    public static final String LOOKUP_TIMEOUT = "quarkus.mongodb.native.dns.lookup-timeout";

    private static DnsClient client;

    public static synchronized io.vertx.mutiny.core.dns.DnsClient createDnsClient() {
        Config config = ConfigProvider.getConfig();
        if (client == null) {
            Vertx vertx = Arc.container().instance(Vertx.class).get();

            boolean activity = config.getOptionalValue(DNS_SERVER_ACTIVITY, Boolean.class).orElse(false);

            // If the server is not set, we attempt to read the /etc/resolv.conf. If it does not exist, we use the default
            // configuration.
            String server = config.getOptionalValue(DNS_SERVER, String.class).orElseGet(() -> {
                List<String> list = nameServers();
                if (!list.isEmpty()) {
                    return list.get(0);
                }
                return null;
            });
            if (server != null) {
                int port = config.getOptionalValue(DNS_SERVER_PORT, Integer.class).orElse(53);
                client = vertx.createDnsClient(new DnsClientOptions().setLogActivity(activity).setPort(port).setHost(server));
            } else {
                client = vertx.createDnsClient(new DnsClientOptions().setLogActivity(activity));
            }
        }

        return client;
    }

    public static List<String> nameServers() {
        String file = "/etc/resolv.conf";
        if (!new File(file).isFile()) {
            return Collections.emptyList();
        }
        Path p = Paths.get(file);
        List<String> nameServers = new ArrayList<>();
        String line;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(Files.newInputStream(p)))) {
            while ((line = br.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line);
                if (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    if (token.startsWith("nameserver")) {
                        // If we do not have another token it means that the line is not formatted correctly.
                        // So, throwing an exception is ok.
                        nameServers.add(st.nextToken().trim());
                    }
                }
            }
        } catch (IOException e) {
            Logger.getLogger(DnsClientProducer.class).error("Unable to read the /etc/resolv.conf file", e);
        }
        return nameServers;
    }
}
