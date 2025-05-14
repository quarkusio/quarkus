package io.quarkus.test.common;

import java.time.Duration;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;

import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.specification.RequestSpecification;

/**
 * Utility class that sets the rest assured port to the default test port and meaningful timeouts.
 * <p>
 * This class works whether RestAssured is on the classpath or not - if it is not, invoking the methods of the class are
 * essentially NO-OPs
 * <p>
 * TODO: do we actually want this here, or should it be in a different module?
 */
public class RestAssuredURLManager {

    private static final int DEFAULT_HTTP_PORT = 8081;
    private static final int DEFAULT_HTTPS_PORT = 8444;

    private static int oldPort;
    private static String oldBaseURI;
    private static String oldBasePath;
    private static Object oldRestAssuredConfig; // we can't declare the type here as that would prevent this class for being loaded if RestAssured is not present
    private static Object oldRequestSpecification;

    private static final boolean REST_ASSURED_PRESENT;

    static {
        boolean present = false;
        try {
            Class.forName("io.restassured.RestAssured");
            present = true;
        } catch (ClassNotFoundException ignored) {
        }
        REST_ASSURED_PRESENT = present;
    }

    private RestAssuredURLManager() {

    }

    private static int getPortFromConfig(int defaultValue, String... keys) {
        for (String key : keys) {
            Optional<Integer> port = ConfigProvider.getConfig().getOptionalValue(key, Integer.class);
            if (port.isPresent())
                return port.get();
        }
        return defaultValue;
    }

    public static void setURL(boolean useSecureConnection) {
        setURL(useSecureConnection, null, null);
    }

    public static void setURL(boolean useSecureConnection, String additionalPath) {
        setURL(useSecureConnection, null, additionalPath);
    }

    public static void setURL(boolean useSecureConnection, Integer port) {
        setURL(useSecureConnection, port, null);
    }

    public static void setURL(boolean useSecureConnection, Integer port, String additionalPath) {
        if (!REST_ASSURED_PRESENT) {
            return;
        }

        oldPort = RestAssured.port;
        if (port == null) {
            port = useSecureConnection ? getPortFromConfig(DEFAULT_HTTPS_PORT, "quarkus.http.test-ssl-port")
                    : getPortFromConfig(DEFAULT_HTTP_PORT, "quarkus.lambda.mock-event-server.test-port",
                            "quarkus.http.test-port");
        }
        RestAssured.port = port;

        oldBaseURI = RestAssured.baseURI;
        final String protocol = useSecureConnection ? "https://" : "http://";
        String host = ConfigProvider.getConfig().getOptionalValue("quarkus.http.host", String.class)
                .orElse("localhost");
        if (host.equals("0.0.0.0")) {
            host = "localhost";
        }
        RestAssured.baseURI = protocol + host;

        oldBasePath = RestAssured.basePath;
        Optional<String> basePath = ConfigProvider.getConfig().getOptionalValue("quarkus.http.root-path",
                String.class);
        if (basePath.isPresent() || additionalPath != null) {
            StringBuilder bp = new StringBuilder();
            if (basePath.isPresent()) {
                if (basePath.get().startsWith("/")) {
                    bp.append(basePath.get().substring(1));
                } else {
                    bp.append(basePath.get());
                }
                if (bp.toString().endsWith("/")) {
                    bp.setLength(bp.length() - 1);
                }
            }
            if (additionalPath != null) {
                if (!additionalPath.startsWith("/")) {
                    bp.append("/");
                }
                bp.append(additionalPath);
                if (bp.toString().endsWith("/")) {
                    bp.setLength(bp.length() - 1);
                }
            }
            RestAssured.basePath = bp.toString();
        }

        oldRestAssuredConfig = RestAssured.config();

        Duration timeout = ConfigProvider.getConfig()
                .getOptionalValue("quarkus.http.test-timeout", Duration.class).orElse(Duration.ofSeconds(30));
        configureTimeouts(timeout);

        oldRequestSpecification = RestAssured.requestSpecification;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    private static void configureTimeouts(Duration d) {
        RestAssured.config = RestAssured.config().httpClient(new HttpClientConfig()
                .setParam("http.conn-manager.timeout", d.toMillis()) // this needs to be long
                .setParam("http.connection.timeout", (int) d.toMillis()) // this needs to be int
                .setParam("http.socket.timeout", (int) d.toMillis())); // same here
    }

    public static void clearURL() {
        if (!REST_ASSURED_PRESENT) {
            return;
        }

        RestAssured.port = oldPort;
        RestAssured.baseURI = oldBaseURI;
        RestAssured.basePath = oldBasePath;
        RestAssured.config = (RestAssuredConfig) oldRestAssuredConfig;
        RestAssured.requestSpecification = (RequestSpecification) oldRequestSpecification;
    }
}
