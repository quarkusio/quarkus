package io.quarkus.infinispan.client.runtime;

import static io.quarkus.infinispan.client.runtime.InfinispanServiceBindingConverter.INFINISPAN_HOSTS;
import static io.quarkus.infinispan.client.runtime.InfinispanServiceBindingConverter.INFINISPAN_PASSWORD;
import static io.quarkus.infinispan.client.runtime.InfinispanServiceBindingConverter.INFINISPAN_URI;
import static io.quarkus.infinispan.client.runtime.InfinispanServiceBindingConverter.INFINISPAN_USERNAME;
import static io.quarkus.infinispan.client.runtime.InfinispanServiceBindingConverter.INFINISPAN_USE_AUTH;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import io.quarkus.kubernetes.service.binding.runtime.ServiceBinding;

public class InfinispanServiceBindingConverterTest {
    private final static Path rootPath = Paths.get("src/test/resources/service-binding");
    private final static String BINDING_DIRECTORY_ALL_PROPS = "all-props";
    private final static String BINDING_DIRECTORY_NO_PROPS = "no-props";
    private static final String EXPECTED_USERNAME = "someadmin";
    private static final String EXPECTED_PASSWORD = "infiniforever";
    private static final String EXPECTED_HOSTS = "infinispan.forever.com:11222";
    private static final String EXPECTED_URI = "hotrod://admin:password@infinispan.forever.com:11222";

    @Test
    public void testBindingWithAllProperties() {
        String bindingDirectory = BINDING_DIRECTORY_ALL_PROPS;
        ServiceBinding serviceBinding = new ServiceBinding(rootPath.resolve(bindingDirectory));
        assertThat(serviceBinding.getName()).isEqualTo(bindingDirectory);
        assertThat(serviceBinding.getType()).isEqualTo("infinispan");
        assertThat(serviceBinding.getProvider()).isEqualTo("coco");
        assertThat(serviceBinding.getProperties().get(INFINISPAN_USE_AUTH)).isEqualTo("true");
        assertThat(serviceBinding.getProperties().get(INFINISPAN_USERNAME)).isEqualTo(EXPECTED_USERNAME);
        assertThat(serviceBinding.getProperties().get(INFINISPAN_PASSWORD)).isEqualTo(EXPECTED_PASSWORD);
        assertThat(serviceBinding.getProperties().get(INFINISPAN_HOSTS)).isEqualTo(EXPECTED_HOSTS);
        assertThat(serviceBinding.getProperties().get(INFINISPAN_URI)).isEqualTo(EXPECTED_URI);
    }

    @Test
    public void testBindingWithMissingProperties() {
        String bindingDirectory = BINDING_DIRECTORY_NO_PROPS;
        ServiceBinding serviceBinding = new ServiceBinding(rootPath.resolve(bindingDirectory));
        assertThat(serviceBinding.getName()).isEqualTo(bindingDirectory);
        assertThat(serviceBinding.getType()).isEqualTo("infinispan");
        assertThat(serviceBinding.getProvider()).isNull();
        assertThat(serviceBinding.getProperties().containsKey(INFINISPAN_USE_AUTH)).isFalse();
        assertThat(serviceBinding.getProperties().containsKey(INFINISPAN_USERNAME)).isFalse();
        assertThat(serviceBinding.getProperties().containsKey(INFINISPAN_PASSWORD)).isFalse();
        assertThat(serviceBinding.getProperties().containsKey(INFINISPAN_HOSTS)).isFalse();
        assertThat(serviceBinding.getProperties().containsKey(INFINISPAN_URI)).isFalse();
    }
}
