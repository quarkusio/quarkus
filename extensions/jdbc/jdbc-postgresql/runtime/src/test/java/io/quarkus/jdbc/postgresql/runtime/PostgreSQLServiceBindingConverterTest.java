package io.quarkus.jdbc.postgresql.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.kubernetes.service.binding.runtime.ServiceBinding;
import io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConfigSource;
import io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConverter;

class PostgreSQLServiceBindingConverterTest {

    private final Path root = Paths.get("src/test/resources/bindings");

    @Test
    public void testConvertAll() {
        List<ServiceBinding> serviceBindings = new ArrayList<>();
        ServiceBinding binding = new ServiceBinding(root.resolve("test-name"));
        serviceBindings.add(binding);

        ServiceBindingConverter c = new PostgreSQLServiceBindingConverter();
        Optional<ServiceBindingConfigSource> conntionProp = c.convert(serviceBindings);
        String sslRootCertPath = root.resolve("test-name").resolve("root.crt").toString();
        String expectedURL = "jdbc:postgresql://aws.crdb-cloud.com:26257/defaultdb?sslmode=verify-full&sslrootcert="
                + sslRootCertPath + "&options=--cluster%3Da-crdb-cluster-0101%20-c%20search_path%3Dkeyword";
        assertThat(conntionProp.get().getProperties().get("quarkus.datasource.jdbc.url")).isEqualTo(expectedURL);
        assertThat(conntionProp.get().getProperties().get("quarkus.datasource.password")).isEqualTo("\\");
        assertThat(conntionProp.get().getProperties().get("quarkus.datasource.username")).isEqualTo("remote-user");
    }

    @Test
    public void testConvertNoOptions() {
        List<ServiceBinding> serviceBindings = new ArrayList<>();
        ServiceBinding binding = new ServiceBinding(root.resolve("no-options"));
        serviceBindings.add(binding);

        ServiceBindingConverter c = new PostgreSQLServiceBindingConverter();
        Optional<ServiceBindingConfigSource> conntionProp = c.convert(serviceBindings);

        String sslRootCertPath = root.resolve("no-options").resolve("root.crt").toString();
        String expectedURL = "jdbc:postgresql://aws.crdb-cloud.com:26257/defaultdb?sslmode=verify-full&sslrootcert="
                + sslRootCertPath;
        assertThat(conntionProp.get().getProperties().get("quarkus.datasource.jdbc.url")).isEqualTo(expectedURL);
        assertThat(conntionProp.get().getProperties().get("quarkus.datasource.password")).isEqualTo("\\");
        assertThat(conntionProp.get().getProperties().get("quarkus.datasource.username")).isEqualTo("remote-user");
    }

    @Test
    public void testConvertDisableSslMode() {
        List<ServiceBinding> serviceBindings = new ArrayList<>();
        ServiceBinding binding = new ServiceBinding(root.resolve("no-ssl"));
        serviceBindings.add(binding);

        ServiceBindingConverter c = new PostgreSQLServiceBindingConverter();
        Optional<ServiceBindingConfigSource> conntionProp = c.convert(serviceBindings);

        String expectedURL = "jdbc:postgresql://aws.crdb-cloud.com:26257/defaultdb?sslmode=disable&options=--cluster%3Da-crdb-cluster-0101";

        assertThat(conntionProp.get().getProperties().get("quarkus.datasource.jdbc.url")).isEqualTo(expectedURL);
        assertThat(conntionProp.get().getProperties().get("quarkus.datasource.password")).isEqualTo("pwd");
        assertThat(conntionProp.get().getProperties().get("quarkus.datasource.username")).isEqualTo("remote-user");
    }
}
