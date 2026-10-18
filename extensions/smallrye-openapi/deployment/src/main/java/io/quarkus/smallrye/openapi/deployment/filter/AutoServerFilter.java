package io.quarkus.smallrye.openapi.deployment.filter;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.servers.Server;

/**
 * Automatically add default server if none is provided
 */
public class AutoServerFilter implements OASFilter {

    private static final String HTTP = "http";
    private static final String ZEROS = "0.0.0.0";
    private static final String LOCALHOST = "localhost";
    private static final String URL_PATTERN = "%s://%s:%d";

    private final String defaultScheme;
    private final String defaultHost;
    private final int defaultPort;
    private final String description;

    public AutoServerFilter(String defaultScheme, String defaultHost, int defaultPort, String description) {
        if (defaultScheme == null) {
            defaultScheme = HTTP;
        }
        if (defaultHost == null) {
            defaultHost = ZEROS;
        }
        this.defaultScheme = defaultScheme;
        this.defaultHost = defaultHost;
        this.defaultPort = defaultPort;
        this.description = description;
    }

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {

        List<Server> servers = openAPI.getServers();
        if (servers == null || servers.isEmpty()) {
            servers = new ArrayList<>();

            // In case of 0.0.0.0, also add localhost
            if (this.defaultHost.equals(ZEROS)) {
                Server localhost = OASFactory.createServer();
                localhost.setUrl(getUrl(this.defaultScheme, LOCALHOST, this.defaultPort));
                localhost.setDescription(this.description);
                servers.add(localhost);
            }

            Server server = OASFactory.createServer();
            server.setUrl(getUrl(this.defaultScheme, this.defaultHost, this.defaultPort));
            server.setDescription(this.description);
            servers.add(server);

            openAPI.setServers(servers);
        }
    }

    private String getUrl(String scheme, String host, int port) {
        return String.format(URL_PATTERN, scheme, host, port);
    }
}
