package io.quarkus.test.common;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Utility class that sets the rest assured port to the default test port and meaningful timeouts.
 * <p>
 * This uses reflection so as to not introduce a dependency on rest-assured
 * <p>
 * TODO: do we actually want this here, or should it be in a different module?
 */
public class RestAssuredURLManager {

    private static final int DEFAULT_HTTP_PORT = 8081;
    private static final int DEFAULT_HTTPS_PORT = 8444;

    private static final Field portField;
    private static final Field baseURIField;
    private static final Field basePathField;
    private static final Field configField;

    private static final Method setParamMethod;
    private static final Method configMethod;
    private static final Method httpClientMethod;
    private static final Class<?> httpClientConfigClass;

    private static int oldPort;
    private static String oldBaseURI;
    private static String oldBasePath;

    static {
        Field p;
        Field baseURI;
        Field basePath;
        Field configF;
        Method setParam;
        Method configM;
        Method httpClient;
        Class<?> httpClientConfig;
        try {
            Class<?> restAssured = Class.forName("io.restassured.RestAssured");
            p = restAssured.getField("port");
            p.setAccessible(true);
            baseURI = restAssured.getField("baseURI");
            baseURI.setAccessible(true);
            basePath = restAssured.getField("basePath");
            basePath.setAccessible(true);

            configF = restAssured.getField("config");
            configF.setAccessible(true);

            configM = restAssured.getMethod("config");
            configM.setAccessible(true);

            httpClientConfig = Class.forName("io.restassured.config.HttpClientConfig");
            setParam = httpClientConfig.getMethod("setParam", String.class, Object.class);
            setParam.setAccessible(true);

            Class<?> restAssuredConfigClass = Class.forName("io.restassured.config.RestAssuredConfig");
            httpClient = restAssuredConfigClass.getMethod("httpClient", httpClientConfig);
            httpClient.setAccessible(true);

        } catch (Exception e) {
            p = null;
            baseURI = null;
            basePath = null;
            configF = null;
            setParam = null;
            httpClientConfig = null;
            configM = null;
            httpClient = null;
        }
        portField = p;
        baseURIField = baseURI;
        basePathField = basePath;
        configField = configF;
        setParamMethod = setParam;
        httpClientConfigClass = httpClientConfig;
        configMethod = configM;
        httpClientMethod = httpClient;
    }

    private RestAssuredURLManager() {

    }

    private static int getPortFromConfig(String key, int defaultValue) {
        return ConfigProvider.getConfig().getOptionalValue(key, Integer.class).orElse(defaultValue);
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
        if (portField != null) {
            try {
                oldPort = (Integer) portField.get(null);
                if (port == null) {
                    port = useSecureConnection ? getPortFromConfig("quarkus.https.test-port", DEFAULT_HTTPS_PORT)
                            : getPortFromConfig("quarkus.http.test-port", DEFAULT_HTTP_PORT);
                }
                portField.set(null, port);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        if (baseURIField != null) {
            try {
                oldBaseURI = (String) baseURIField.get(null);
                final String protocol = useSecureConnection ? "https://" : "http://";
                String host = ConfigProvider.getConfig().getOptionalValue("quarkus.http.host", String.class)
                        .orElse("localhost");
                if (host.equals("0.0.0.0")) {
                    host = "localhost";
                }
                String baseURI = protocol + host;
                baseURIField.set(null, baseURI);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        if (basePathField != null) {
            try {
                oldBasePath = (String) basePathField.get(null);
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
                    basePathField.set(null, bp.toString());
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        if (configField != null) {
            try {
                Duration timeout = ConfigProvider.getConfig()
                        .getOptionalValue("quarkus.http.test-timeout", Duration.class).orElse(Duration.ofSeconds(30));
                configureTimeouts(timeout);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Execute the following code:
     * 
     * <pre>
     * {@code
     * RestAssured.config = RestAssured.config().httpClient(
     * new HttpClientConfig()
     * .setParam("http.conn-manager.timeout", 30_000L)
     * .setParam("http.connection.timeout", 30_000)
     * .setParam("http.socket.timeout", 30_000)
     * }
     * </pre>
     *
     */
    private static void configureTimeouts(Duration d) throws Exception {
        // Create the HTTP client config:
        // new HttpClientConfig()
        //   .setParam("http.conn-manager.timeout", 30_000L)
        //   .setParam("http.connection.timeout", 30_000)
        //   .setParam("http.socket.timeout", 30_000)

        Object httpClientConfig = httpClientConfigClass.getDeclaredConstructor().newInstance();
        httpClientConfig = setParamMethod.invoke(httpClientConfig, "http.conn-manager.timeout", d.toMillis());
        httpClientConfig = setParamMethod.invoke(httpClientConfig, "http.connection.timeout", (int) d.toMillis());
        httpClientConfig = setParamMethod.invoke(httpClientConfig, "http.socket.timeout", (int) d.toMillis());

        // Retrieve RestAssured config
        // restAssuredConfig = RestAssured.config();
        Object restAssuredConfig = configMethod.invoke(null);

        // Call httpClient method:
        // restAssuredConfig = restAssuredConfig.httpClient(httpClientConfig);
        restAssuredConfig = httpClientMethod.invoke(restAssuredConfig, httpClientConfig);

        // Set the field
        configField.set(null, restAssuredConfig);
    }

    public static void clearURL() {
        if (portField != null) {
            try {
                portField.set(null, oldPort);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        if (baseURIField != null) {
            try {
                baseURIField.set(null, oldBaseURI);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        if (basePathField != null) {
            try {
                basePathField.set(null, oldBasePath);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
