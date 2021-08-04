package io.quarkus.mongodb.runtime;

import static java.lang.String.format;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkus.kubernetes.service.binding.runtime.ServiceBinding;
import io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConfigSource;
import io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConverter;

/**
 * ServiceBindingConverter for MongoDB to support SBO (Service Binding Operator) in Quarkus.
 *
 * This class supports both the <b>Standard</b> and <b>SRV</b> connection string format for
 * MongoDB depending on whether port is provided or not.
 *
 * <br>
 * Following individual properties are supported to make the connection string:
 * <ul>
 * <li>username</li>
 * <li>password</li>
 * <li>host</li>
 * <li>port</li>
 * <li>database</li>
 * </ul>
 * <i>Other than host all other properties are optinoal</i>
 *
 * <br>
 * <br>
 * Only following options are supported by default:
 * <ul>
 * <li>retryWrites=true</li>
 * <li>w=majority</li>
 * </ul>
 *
 */
public class MongoServiceBindingConverter implements ServiceBindingConverter {
    private static final Logger LOGGER = Logger.getLogger(MongoServiceBindingConverter.class);

    private static final String BINDING_TYPE = "mongodb";
    public static final String BINDING_CONFIG_SOURCE_NAME = BINDING_TYPE + "-k8s-service-binding-source";
    public static final String MONGO_DB_CONNECTION_STRING = "quarkus.mongodb.connection-string";

    // DB properties
    public static final String DB_USER = "username";
    public static final String DB_PASSWORD = "password";
    public static final String DB_HOST = "host";
    public static final String DB_PORT = "port";
    public static final String DB_DATABASE = "database";
    public static final String DB_PREFIX_STANDARD = "mongodb://";
    public static final String DB_PREFIX_SRV = "mongodb+srv://";
    public static final String DB_DEFAULT_OPTIONS = "?retryWrites=true&w=majority";

    private static final String CONNECTION_STRING_WITH_USER = "%s%s:%s@%s/%s" + DB_DEFAULT_OPTIONS; // user:pass@hostPort/database
    private static final String CONNECTION_STRING_WITH_USER_NO_DB = "%s%s:%s@%s" + DB_DEFAULT_OPTIONS; // user:pass@hostPort
    private static final String CONNECTION_STRING_WITHOUT_USER = "%s%s/%s" + DB_DEFAULT_OPTIONS; // hostPort/database
    private static final String CONNECTION_STRING_WITHOUT_USER_NO_DB = "%s%s" + DB_DEFAULT_OPTIONS; // hostPort

    @Override
    public Optional<ServiceBindingConfigSource> convert(List<ServiceBinding> serviceBindings) {
        Optional<ServiceBinding> matchingByType = ServiceBinding.singleMatchingByType(BINDING_TYPE, serviceBindings);
        if (matchingByType.isEmpty()) {
            return Optional.empty();
        }

        Map<String, String> properties = new HashMap<>();

        properties.put(MONGO_DB_CONNECTION_STRING, getConnectionString(matchingByType.get()));

        return Optional.of(new ServiceBindingConfigSource(BINDING_CONFIG_SOURCE_NAME, properties));
    }

    private String getConnectionString(ServiceBinding binding) {
        String user = getDbProperty(binding, DB_USER);
        String password = getDbProperty(binding, DB_PASSWORD);
        String hostPort = getHostPort(binding);
        String database = getDbProperty(binding, DB_DATABASE);
        String prefix = isPortProvided(hostPort) ? DB_PREFIX_STANDARD : DB_PREFIX_SRV;

        String connectionString;
        if (isBlank(user)) {
            if (isBlank(database)) {
                connectionString = format(CONNECTION_STRING_WITHOUT_USER_NO_DB, prefix, hostPort);
            } else {
                connectionString = format(CONNECTION_STRING_WITHOUT_USER, prefix, hostPort, database);
            }
        } else {
            if (isBlank(database)) {
                connectionString = format(CONNECTION_STRING_WITH_USER_NO_DB, prefix, user, password, hostPort);
            } else {
                connectionString = format(CONNECTION_STRING_WITH_USER, prefix, user, password, hostPort, database);
            }
        }

        LOGGER.info(format("MongoDB connection string: [%s]", connectionString));
        return connectionString;
    }

    private String getDbProperty(ServiceBinding binding, String dbPropertyKey) {
        String dbPropertyValue = binding.getProperties().get(dbPropertyKey);
        if (isBlank(dbPropertyValue)) {
            LOGGER.debug(format("Property '%s' not found", dbPropertyKey));
        }

        return dbPropertyValue;
    }

    private String getHostPort(ServiceBinding binding) {
        String host = getDbProperty(binding, DB_HOST);
        String port = getDbProperty(binding, DB_PORT);

        String hostPort = host;
        if (isNotBlank(host)) {
            if (isNotBlank(port)) {
                hostPort = host + ":" + port;
            }
        } else {
            LOGGER.warn("Unable to get the host property. Connection string won't be correct");
        }

        return hostPort;
    }

    private boolean isPortProvided(String hostPort) {
        return hostPort != null && hostPort.contains(":");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private boolean isNotBlank(String value) {
        return !isBlank(value);
    }
}