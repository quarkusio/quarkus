package io.quarkus.mongodb.runtime;

import static java.lang.Boolean.parseBoolean;
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
 * MongoDB depending on whether <b>srv</b> property is true or false. If the srv property is
 * missing then it is same as having a value of false.
 *
 * <br>
 * <br>
 * Following individual properties are supported to make the connection string:
 * <ul>
 * <li>username</li>
 * <li>password</li>
 * <li>host (<i>port information can be provided in this property separated by : sign</i>, e.g. localhost:27010</li>
 * <li>database</li>
 * <li>srv</li>
 * <li>options</li>
 * </ul>
 * <i>Other than host all other properties are optional</i>
 *
 * <br>
 * <br>
 * The Quarkus properties set by this class are:
 * <ul>
 * <li>quarkus.mongodb.connection-string</li>
 * <li>quarkus.mongodb.credentials.username (<i>if username is provided</i>)</li>
 * <li>quarkus.mongodb.credentials.password (<i>if password is provided</i>)</li>
 * </ul>
 */
public class MongoServiceBindingConverter implements ServiceBindingConverter {
    private static final Logger LOGGER = Logger.getLogger(MongoServiceBindingConverter.class);

    private static final String BINDING_TYPE = "mongodb";
    public static final String BINDING_CONFIG_SOURCE_NAME = BINDING_TYPE + "-k8s-service-binding-source";
    public static final String MONGO_DB_CONNECTION_STRING = "quarkus.mongodb.connection-string";
    public static final String MONGO_DB_USERNAME = "quarkus.mongodb.credentials.username";
    public static final String MONGO_DB_PASSWORD = "quarkus.mongodb.credentials.password";

    // DB properties
    public static final String DB_USER = "username";
    public static final String DB_PASSWORD = "password";
    public static final String DB_HOST = "host";
    public static final String DB_DATABASE = "database";
    public static final String DB_OPTIONS = "options";
    public static final String DB_PREFIX_STANDARD = "mongodb://";
    public static final String DB_PREFIX_SRV = "mongodb+srv://";
    public static final String DB_SRV = "srv";

    // 1st format specifier is for the prefix in each of the following ConnectionString
    private static final String CONNECTION_STRING_WITH_HOST_N_DB = "%s%s/%s"; // hostPort/database
    private static final String CONNECTION_STRING_WITH_ONLY_HOST = "%s%s"; // hostPort

    @Override
    public Optional<ServiceBindingConfigSource> convert(List<ServiceBinding> serviceBindings) {
        Optional<ServiceBinding> matchingByType = ServiceBinding.singleMatchingByType(BINDING_TYPE, serviceBindings);
        if (matchingByType.isEmpty()) {
            return Optional.empty();
        }

        ServiceBinding binding = matchingByType.get();
        Map<String, String> properties = new HashMap<>();

        setConnectionString(binding, properties);
        setUsername(binding, properties);
        setPassword(binding, properties);

        return Optional.of(new ServiceBindingConfigSource(BINDING_CONFIG_SOURCE_NAME, properties));
    }

    private void setConnectionString(ServiceBinding binding, Map<String, String> properties) {
        String hostPort = getHost(binding);
        String prefix = isSrv(binding) ? DB_PREFIX_SRV : DB_PREFIX_STANDARD;
        String database = getDbProperty(binding, DB_DATABASE);
        String options = getDbProperty(binding, DB_OPTIONS);
        boolean isOptionsNotBlank = isNotBlank(options);
        String connectionString;

        if (isBlank(database)) {
            connectionString = format(CONNECTION_STRING_WITH_ONLY_HOST, prefix, hostPort);

            if (isOptionsNotBlank) {
                // We need a trailing slash before options otherwise Mongo throws "contains options without trailing slash" error
                // If the database value is not present, then we haven't yet added the trailing slash (and hence add it here)
                connectionString += "/";
            }
        } else {
            connectionString = format(CONNECTION_STRING_WITH_HOST_N_DB, prefix, hostPort, database);
        }

        if (isOptionsNotBlank) {
            connectionString += "?" + options;
        }

        LOGGER.debug(format("MongoDB connection string: [%s]", connectionString));

        properties.put(MONGO_DB_CONNECTION_STRING, connectionString);
    }

    private void setUsername(ServiceBinding binding, Map<String, String> properties) {
        String username = getDbProperty(binding, DB_USER);
        LOGGER.debug(format("MongoDB username=%s", username));

        properties.put(MONGO_DB_USERNAME, username);
    }

    private void setPassword(ServiceBinding binding, Map<String, String> properties) {
        properties.put(MONGO_DB_PASSWORD, getDbProperty(binding, DB_PASSWORD));
    }

    private String getDbProperty(ServiceBinding binding, String dbPropertyKey) {
        String dbPropertyValue = binding.getProperties().get(dbPropertyKey);
        if (isBlank(dbPropertyValue)) {
            LOGGER.debug(format("Property '%s' not found", dbPropertyKey));
        }

        return dbPropertyValue;
    }

    private String getHost(ServiceBinding binding) {
        String host = getDbProperty(binding, DB_HOST);

        if (isBlank(host)) {
            LOGGER.warn("Unable to get the host property. Connection string won't be correct");
        }

        return host;
    }

    private boolean isSrv(ServiceBinding binding) {
        String srv = getDbProperty(binding, DB_SRV);

        return isNotBlank(srv) && parseBoolean(srv);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private boolean isNotBlank(String value) {
        return !isBlank(value);
    }
}