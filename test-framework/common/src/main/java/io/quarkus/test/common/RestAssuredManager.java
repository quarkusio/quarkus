package io.quarkus.test.common;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Utility class that sets the rest assured port to the default test port.
 * It also configures RestAssured to use the ObjectMapper configured by the application,
 * if there is such an ObjectMapper
 * <p>
 * This uses reflection so as to not introduce a dependency on rest-assured
 * <p>
 * TODO: do we actually want this here, or should it be in a different module?
 */
public class RestAssuredManager {

    private static final int DEFAULT_HTTP_PORT = 8081;
    private static final int DEFAULT_HTTPS_PORT = 8444;

    private static final Field portField;
    private static final Field baseURIField;
    private static final Field basePathField;
    private static final Field configField;
    private static Object newRestAssuredConfig;
    private int oldPort;
    private String oldBaseURI;
    private String oldBasePath;
    private Object oldRestAssuredConfig;

    private final boolean useSecureConnection;

    static {
        Field p;
        Field baseURI;
        Field basePath;
        Field config;
        Class<?> restAssured;
        try {
            restAssured = Class.forName("io.restassured.RestAssured");
            p = restAssured.getField("port");
            p.setAccessible(true);
            baseURI = restAssured.getField("baseURI");
            baseURI.setAccessible(true);
            basePath = restAssured.getField("basePath");
            basePath.setAccessible(true);
            config = restAssured.getField("config");
            config.setAccessible(true);
        } catch (Exception e) {
            p = null;
            baseURI = null;
            basePath = null;
            config = null;
        }
        portField = p;
        baseURIField = baseURI;
        basePathField = basePath;
        configField = config;
        newRestAssuredConfig = null;
        if (configField != null) {
            setNewRestAssuredConfig();
        }
    }

    /**
     * Configure RestAssured to use the application's ObjectMapper if it's set
     */
    private static void setNewRestAssuredConfig() {
        try {
            // config = RestAssuredConfig.config().objectMapperConfig(new ObjectMapperConfig().jackson2ObjectMapperFactory(new QuarkusJackson2ObjectMapperFactory());
            Class<?> restAssuredConfigClass = Class.forName("io.restassured.config.RestAssuredConfig");
            Method configMethod = restAssuredConfigClass.getMethod("config");
            Object newConfig = configMethod.invoke(null);
            Class<?> objectMapperConfigClass = Class.forName("io.restassured.config.ObjectMapperConfig");
            Object newObjectMapperConfig = objectMapperConfigClass.newInstance();
            Method jackson2ObjectMapperFactoryMethod = objectMapperConfigClass.getMethod("jackson2ObjectMapperFactory",
                    Class.forName("io.restassured.path.json.mapper.factory.Jackson2ObjectMapperFactory"));
            Object quarkusJackson2ObjectMapperFactory = Class
                    .forName("io.quarkus.test.restassured.QuarkusJackson2ObjectMapperFactory").newInstance();
            newObjectMapperConfig = jackson2ObjectMapperFactoryMethod.invoke(newObjectMapperConfig,
                    quarkusJackson2ObjectMapperFactory);
            Method objectMapperConfigMethod = restAssuredConfigClass.getMethod("objectMapperConfig",
                    objectMapperConfigClass);
            newRestAssuredConfig = objectMapperConfigMethod.invoke(newConfig, newObjectMapperConfig);
        } catch (Exception e) {
        }
    }

    public RestAssuredManager(boolean useSecureConnection) {
        this.useSecureConnection = useSecureConnection;
    }

    private static int getPortFromConfig(String key, int defaultValue) {
        return ConfigProvider.getConfig().getOptionalValue(key, Integer.class).orElse(defaultValue);
    }

    public void set() {
        if (portField != null) {
            try {
                oldPort = (Integer) portField.get(null);
                int port = useSecureConnection ? getPortFromConfig("quarkus.https.test-port", DEFAULT_HTTPS_PORT)
                        : getPortFromConfig("quarkus.http.test-port", DEFAULT_HTTP_PORT);
                portField.set(null, port);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        if (baseURIField != null) {
            try {
                oldBaseURI = (String) baseURIField.get(null);
                final String protocol = useSecureConnection ? "https://" : "http://";
                String baseURI = protocol + ConfigProvider.getConfig().getOptionalValue("quarkus.http.host", String.class)
                        .orElse("localhost");
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
                if (basePath.isPresent()) {
                    basePathField.set(null, basePath.get());
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        if ((configField != null) && newRestAssuredConfig != null) {
            try {
                oldRestAssuredConfig = configField.get(null);
                configField.set(null, newRestAssuredConfig);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public void clear() {
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
        if ((configField != null) && newRestAssuredConfig != null) {
            try {
                configField.set(null, oldRestAssuredConfig);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
