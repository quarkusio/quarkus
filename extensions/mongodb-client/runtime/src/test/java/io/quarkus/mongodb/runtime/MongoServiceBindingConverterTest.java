package io.quarkus.mongodb.runtime;

import static io.quarkus.mongodb.runtime.MongoServiceBindingConverter.BINDING_CONFIG_SOURCE_NAME;
import static io.quarkus.mongodb.runtime.MongoServiceBindingConverter.DB_DATABASE;
import static io.quarkus.mongodb.runtime.MongoServiceBindingConverter.DB_HOST;
import static io.quarkus.mongodb.runtime.MongoServiceBindingConverter.DB_OPTIONS;
import static io.quarkus.mongodb.runtime.MongoServiceBindingConverter.DB_PASSWORD;
import static io.quarkus.mongodb.runtime.MongoServiceBindingConverter.DB_PREFIX_SRV;
import static io.quarkus.mongodb.runtime.MongoServiceBindingConverter.DB_PREFIX_STANDARD;
import static io.quarkus.mongodb.runtime.MongoServiceBindingConverter.DB_SRV;
import static io.quarkus.mongodb.runtime.MongoServiceBindingConverter.DB_USER;
import static io.quarkus.mongodb.runtime.MongoServiceBindingConverter.MONGO_DB_CONNECTION_STRING;
import static io.quarkus.mongodb.runtime.MongoServiceBindingConverter.MONGO_DB_PASSWORD;
import static io.quarkus.mongodb.runtime.MongoServiceBindingConverter.MONGO_DB_USERNAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;

import io.quarkus.kubernetes.service.binding.runtime.ServiceBinding;
import io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConfigSource;

public class MongoServiceBindingConverterTest {
    private final static Path rootPath = Paths.get("src/test/resources/service-binding");
    private final static String BINDING_DIRECTORY_ALL_PROPS = "all-props";
    private final static String BINDING_DIRECTORY_NO_PORT = "no-port";
    private final static String BINDING_DIRECTORY_NO_USER = "no-user";

    private static final String DB_PORT = "port";

    private static final String EXPECTED_USERNAME = "someUserName";
    private static final String EXPECTED_PASSWORD = "password123 isItAGoodPassword 7@%|?^B6";
    private static final String EXPECTED_HOST = "mongodb0.example.com";
    private static final String EXPECTED_STANDARD_HOST_N_PORT_2 = "mongodb0.example.com:27017, mongodb1.example.com:27017, mongodb2.example.com:27017";
    private static final String EXPECTED_DB = "random-DB";
    private static final String EXPECTED_PORT = "11010";
    private static final String EXPECTED_HOST_N_PORT = EXPECTED_HOST + ":" + EXPECTED_PORT;
    private static final String DB_N_OPTIONS_SEPARATOR = "/";
    private static final String OPTIONS_SEPARATOR = "?";
    private static final String EXPECTED_OPTIONS = "retryWrites=true&w=majority";
    private static final String EXPECTED_SRV = "true";

    @Test
    public void testBindingWithAllProperties() {
        String bindingDirectory = BINDING_DIRECTORY_ALL_PROPS;
        ServiceBinding serviceBinding = new ServiceBinding(rootPath.resolve(bindingDirectory));
        assertThat(serviceBinding.getName()).isEqualTo(bindingDirectory);
        assertThat(serviceBinding.getType()).isEqualTo("mongodb");
        assertThat(serviceBinding.getProvider()).isEqualTo("atlas");
        assertThat(serviceBinding.getProperties().get(DB_USER)).isEqualTo(EXPECTED_USERNAME);
        assertThat(serviceBinding.getProperties().get(DB_PASSWORD)).isEqualTo(EXPECTED_PASSWORD);
        assertThat(serviceBinding.getProperties().get(DB_HOST)).isEqualTo(EXPECTED_HOST_N_PORT);
        assertThat(serviceBinding.getProperties().get(DB_PORT)).isNull();
        assertThat(serviceBinding.getProperties().get(DB_DATABASE)).isEqualTo(EXPECTED_DB);
        assertThat(serviceBinding.getProperties().get(DB_OPTIONS)).isEqualTo(EXPECTED_OPTIONS);
    }

    @Test
    public void testBindingWithMissingProperties() {
        String bindingDirectory = BINDING_DIRECTORY_NO_PORT;
        ServiceBinding serviceBinding = new ServiceBinding(rootPath.resolve(bindingDirectory));
        assertThat(serviceBinding.getName()).isEqualTo(bindingDirectory);
        assertThat(serviceBinding.getType()).isEqualTo("mongodb");
        assertThat(serviceBinding.getProvider()).isEqualTo("atlas");
        assertThat(serviceBinding.getProperties().get(DB_USER)).isEqualTo(EXPECTED_USERNAME);
        assertThat(serviceBinding.getProperties().get(DB_PASSWORD)).isEqualTo(EXPECTED_PASSWORD);
        assertThat(serviceBinding.getProperties().get(DB_HOST)).isEqualTo(EXPECTED_HOST);
        assertThat(serviceBinding.getProperties().get(DB_PORT)).isNull();
        assertThat(serviceBinding.getProperties().get(DB_DATABASE)).isEqualTo(EXPECTED_DB);
        assertThat(serviceBinding.getProperties().get(DB_OPTIONS)).isEqualTo(EXPECTED_OPTIONS);
    }

    @Test
    public void testConnectionStringWithPortPresent() {
        HashMap<String, String> properties = getMapWithAllValuesPopulated(EXPECTED_HOST_N_PORT);
        properties.remove(DB_SRV); // Make it a Standard connection string

        ServiceBindingConfigSource serviceBindingConfigSource = readBindingAsSpyAndVerifyConfigSource(
                BINDING_DIRECTORY_ALL_PROPS, properties);

        String expectedConnString = DB_PREFIX_STANDARD + EXPECTED_HOST_N_PORT + DB_N_OPTIONS_SEPARATOR + EXPECTED_DB
                + OPTIONS_SEPARATOR + EXPECTED_OPTIONS;
        verifyConnectionString(serviceBindingConfigSource, DB_PREFIX_STANDARD, expectedConnString);
        verifyUsernameAndPassword(serviceBindingConfigSource);
    }

    @Test
    public void testSrvConnectionStringWithPortMissing() {
        HashMap<String, String> properties = getMapWithAllValuesPopulated(EXPECTED_HOST);

        ServiceBindingConfigSource serviceBindingConfigSource = readBindingAsSpyAndVerifyConfigSource(BINDING_DIRECTORY_NO_PORT,
                properties);

        String expectedConnString = DB_PREFIX_SRV + EXPECTED_HOST + DB_N_OPTIONS_SEPARATOR + EXPECTED_DB + OPTIONS_SEPARATOR
                + EXPECTED_OPTIONS;
        verifyConnectionString(serviceBindingConfigSource, DB_PREFIX_SRV, expectedConnString);
        verifyUsernameAndPassword(serviceBindingConfigSource);
    }

    @Test
    public void testStandardConnectionStringWithPortMissing() {
        HashMap<String, String> properties = getMapWithAllValuesPopulated(EXPECTED_HOST);
        properties.remove(DB_SRV); // Make it a Standard connection string

        ServiceBindingConfigSource serviceBindingConfigSource = readBindingAsSpyAndVerifyConfigSource(BINDING_DIRECTORY_NO_PORT,
                properties);

        String expectedConnString = DB_PREFIX_STANDARD + EXPECTED_HOST + DB_N_OPTIONS_SEPARATOR + EXPECTED_DB
                + OPTIONS_SEPARATOR + EXPECTED_OPTIONS;
        verifyConnectionString(serviceBindingConfigSource, DB_PREFIX_STANDARD, expectedConnString);
        verifyUsernameAndPassword(serviceBindingConfigSource);
    }

    @Test
    public void testStandardConnectionStringWithMultipleHostAndPort() {
        HashMap<String, String> properties = getMapWithAllValuesPopulated(EXPECTED_STANDARD_HOST_N_PORT_2);
        properties.remove(DB_SRV); // Make it a Standard connection string

        ServiceBindingConfigSource serviceBindingConfigSource = readBindingAsSpyAndVerifyConfigSource(BINDING_DIRECTORY_NO_PORT,
                properties);

        String expectedConnString = DB_PREFIX_STANDARD + EXPECTED_STANDARD_HOST_N_PORT_2 + DB_N_OPTIONS_SEPARATOR + EXPECTED_DB
                + OPTIONS_SEPARATOR + EXPECTED_OPTIONS;
        verifyConnectionString(serviceBindingConfigSource, DB_PREFIX_STANDARD, expectedConnString);
        verifyUsernameAndPassword(serviceBindingConfigSource);
    }

    @Test
    public void testConnectionStringWithPortAndDatabaseMissing() {
        HashMap<String, String> properties = getMapWithAllValuesPopulated(EXPECTED_HOST);
        properties.remove(DB_DATABASE); // This is what we actually want to test out

        ServiceBindingConfigSource serviceBindingConfigSource = readBindingAsSpyAndVerifyConfigSource(
                BINDING_DIRECTORY_NO_PORT, properties);

        String expectedConnString = DB_PREFIX_SRV + EXPECTED_HOST + DB_N_OPTIONS_SEPARATOR + OPTIONS_SEPARATOR
                + EXPECTED_OPTIONS;
        verifyConnectionString(serviceBindingConfigSource, DB_PREFIX_SRV, expectedConnString);
        verifyUsernameAndPassword(serviceBindingConfigSource);
    }

    @Test
    public void testConnectionStringWithUserAndOptionsMissing() {
        ServiceBindingConfigSource serviceBindingConfigSource = readBindingAndVerifyConfigSource(BINDING_DIRECTORY_NO_USER);

        String expectedConnString = DB_PREFIX_STANDARD + EXPECTED_HOST + ":" + EXPECTED_PORT + DB_N_OPTIONS_SEPARATOR
                + EXPECTED_DB;
        verifyConnectionString(serviceBindingConfigSource, DB_PREFIX_STANDARD, expectedConnString);
    }

    @Test
    public void testConnectionStringWithUserMissing() {
        HashMap<String, String> properties = getMapWithAllValuesPopulated(EXPECTED_HOST_N_PORT);
        properties.remove(DB_SRV); // BindingDirectory doesn't have this property, but removing it since it's put back in the map
        properties.remove(DB_USER); // This is what we actually want to test out

        ServiceBindingConfigSource serviceBindingConfigSource = readBindingAsSpyAndVerifyConfigSource(BINDING_DIRECTORY_NO_USER,
                properties);

        String expectedConnString = DB_PREFIX_STANDARD + EXPECTED_HOST + ":" + EXPECTED_PORT + DB_N_OPTIONS_SEPARATOR
                + EXPECTED_DB + OPTIONS_SEPARATOR + EXPECTED_OPTIONS;
        verifyConnectionString(serviceBindingConfigSource, DB_PREFIX_STANDARD, expectedConnString);
    }

    @Test
    public void testConnectionStringWithUserAndPortMissing() {
        HashMap<String, String> properties = getMapWithAllValuesPopulated(EXPECTED_HOST);
        properties.remove(DB_USER); // BindingDirectory doesn't have this property, but removing it since it's put back in the map

        ServiceBindingConfigSource serviceBindingConfigSource = readBindingAsSpyAndVerifyConfigSource(
                BINDING_DIRECTORY_NO_USER, properties);

        String expectedConnString = DB_PREFIX_SRV + EXPECTED_HOST + DB_N_OPTIONS_SEPARATOR + EXPECTED_DB
                + OPTIONS_SEPARATOR + EXPECTED_OPTIONS;
        verifyConnectionString(serviceBindingConfigSource, DB_PREFIX_SRV, expectedConnString);
    }

    @Test
    public void testConnectionStringWithUserAndPortAndDatabaseMissing() {
        HashMap<String, String> properties = getMapWithAllValuesPopulated(EXPECTED_HOST);
        properties.remove(DB_USER); // BindingDirectory doesn't have this property, but removing it since it's put back in the map
        properties.remove(DB_DATABASE); // This is what we actually want to test out

        ServiceBindingConfigSource serviceBindingConfigSource = readBindingAsSpyAndVerifyConfigSource(
                BINDING_DIRECTORY_NO_USER, properties);

        String expectedConnString = DB_PREFIX_SRV + EXPECTED_HOST + DB_N_OPTIONS_SEPARATOR + OPTIONS_SEPARATOR
                + EXPECTED_OPTIONS;
        verifyConnectionString(serviceBindingConfigSource, DB_PREFIX_SRV, expectedConnString);
    }

    @Test
    public void testStandardConnectionStringWithUserAndDatabaseMissing() {
        HashMap<String, String> properties = getMapWithAllValuesPopulated(EXPECTED_HOST_N_PORT);
        properties.remove(DB_USER); // BindingDirectory doesn't have this property, but removing it since it's put back in the map
        properties.remove(DB_SRV);
        properties.remove(DB_DATABASE); // This is what we actually want to test out

        ServiceBindingConfigSource serviceBindingConfigSource = readBindingAsSpyAndVerifyConfigSource(
                BINDING_DIRECTORY_NO_USER, properties);

        String expectedConnString = DB_PREFIX_STANDARD + EXPECTED_HOST_N_PORT + DB_N_OPTIONS_SEPARATOR
                + OPTIONS_SEPARATOR + EXPECTED_OPTIONS;
        verifyConnectionString(serviceBindingConfigSource, DB_PREFIX_STANDARD, expectedConnString);
    }

    @Test
    public void testSrvConnectionStringWithUserAndDatabaseMissing() {
        HashMap<String, String> properties = getMapWithAllValuesPopulated(EXPECTED_HOST_N_PORT);
        properties.remove(DB_USER); // BindingDirectory doesn't have this property, but removing it since it's put back in the map
        properties.remove(DB_DATABASE); // This is what we actually want to test out

        ServiceBindingConfigSource serviceBindingConfigSource = readBindingAsSpyAndVerifyConfigSource(
                BINDING_DIRECTORY_NO_USER, properties);

        String expectedConnString = DB_PREFIX_SRV + EXPECTED_HOST_N_PORT + DB_N_OPTIONS_SEPARATOR
                + OPTIONS_SEPARATOR + EXPECTED_OPTIONS;
        verifyConnectionString(serviceBindingConfigSource, DB_PREFIX_SRV, expectedConnString);
    }

    @Test
    public void testStandardConnectionStringWithDatabaseMissing() {
        HashMap<String, String> properties = getMapWithAllValuesPopulated(EXPECTED_HOST_N_PORT);
        properties.remove(DB_SRV); // BindingDirectory doesn't have this property, but removing it since it's put back in the map
        properties.remove(DB_DATABASE); // This is what we actually want to test out

        ServiceBindingConfigSource serviceBindingConfigSource = readBindingAsSpyAndVerifyConfigSource(
                BINDING_DIRECTORY_ALL_PROPS, properties);

        String expectedConnString = DB_PREFIX_STANDARD +
                EXPECTED_HOST_N_PORT + DB_N_OPTIONS_SEPARATOR + OPTIONS_SEPARATOR + EXPECTED_OPTIONS;
        verifyConnectionString(serviceBindingConfigSource, DB_PREFIX_STANDARD, expectedConnString);

        verifyUsernameAndPassword(serviceBindingConfigSource);
    }

    private void verifyUsernameAndPassword(ServiceBindingConfigSource serviceBindingConfigSource) {
        assertThat(serviceBindingConfigSource.getProperties().get(MONGO_DB_USERNAME)).isEqualTo(EXPECTED_USERNAME);
        assertThat(serviceBindingConfigSource.getProperties().get(MONGO_DB_PASSWORD)).isEqualTo(EXPECTED_PASSWORD);
    }

    @Test
    public void testSrvConnectionStringWithDatabaseMissing() {
        HashMap<String, String> properties = getMapWithAllValuesPopulated(EXPECTED_HOST_N_PORT);
        properties.remove(DB_DATABASE); // This is what we actually want to test out

        ServiceBindingConfigSource serviceBindingConfigSource = readBindingAsSpyAndVerifyConfigSource(
                BINDING_DIRECTORY_ALL_PROPS, properties);

        String expectedConnString = DB_PREFIX_SRV + EXPECTED_HOST_N_PORT + DB_N_OPTIONS_SEPARATOR + OPTIONS_SEPARATOR
                + EXPECTED_OPTIONS;
        verifyConnectionString(serviceBindingConfigSource, DB_PREFIX_SRV, expectedConnString);
        verifyUsernameAndPassword(serviceBindingConfigSource);
    }

    private ServiceBindingConfigSource readBindingAndVerifyConfigSource(String bindingDirectory) {
        ServiceBinding serviceBinding = new ServiceBinding(rootPath.resolve(bindingDirectory));
        assertThat(serviceBinding.getName()).isEqualTo(bindingDirectory);

        return verifyConfigSource(serviceBinding);
    }

    /*
     * ServiceBinding will be created as a {@link Mockito.spy} so that we can return the given bindingProperties from
     * that object when getProperties is invoked.
     * This is to facilitate testing any scenario where we don't want to read from binding
     */
    private ServiceBindingConfigSource readBindingAsSpyAndVerifyConfigSource(String bindingDirectory,
            Map<String, String> bindingProperties) {
        ServiceBinding serviceBinding = spy(new ServiceBinding(rootPath.resolve(bindingDirectory)));
        assertThat(serviceBinding.getName()).isEqualTo(bindingDirectory);

        doReturn(bindingProperties).when(serviceBinding).getProperties();

        return verifyConfigSource(serviceBinding);
    }

    private ServiceBindingConfigSource verifyConfigSource(ServiceBinding serviceBinding) {
        Optional<ServiceBindingConfigSource> result = new MongoServiceBindingConverter()
                .convert(Lists.newArrayList(serviceBinding));
        assertThat(result.isPresent()).isTrue();

        ServiceBindingConfigSource serviceBindingConfigSource = result.get();
        assertThat(serviceBindingConfigSource).isNotNull();
        assertThat(serviceBindingConfigSource.getName()).isEqualTo(BINDING_CONFIG_SOURCE_NAME);

        return serviceBindingConfigSource;
    }

    private void verifyConnectionString(ServiceBindingConfigSource serviceBindingConfigSource, String expectedPrefix,
            String expectedConnString) {
        assertThat(serviceBindingConfigSource.getProperties().get(MONGO_DB_CONNECTION_STRING)).startsWith(expectedPrefix);
        assertThat(serviceBindingConfigSource.getProperties().get(MONGO_DB_CONNECTION_STRING)).isEqualTo(expectedConnString);
    }

    private HashMap<String, String> getMapWithAllValuesPopulated(String host) {
        HashMap<String, String> map = new HashMap<>();
        map.put(DB_USER, EXPECTED_USERNAME);
        map.put(DB_PASSWORD, EXPECTED_PASSWORD);
        map.put(DB_HOST, host);
        map.put(DB_DATABASE, EXPECTED_DB);
        map.put(DB_OPTIONS, EXPECTED_OPTIONS);
        map.put(DB_SRV, EXPECTED_SRV);

        return map;
    }
}