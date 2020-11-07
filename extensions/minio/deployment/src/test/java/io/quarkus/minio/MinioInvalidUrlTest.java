package io.quarkus.minio;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.minio.MinioClient;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

class MinioInvalidUrlTest {

    @Inject
    MinioClient minioClient;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-invalid-url.properties");

    @Test
    public void invalidUrlThrowsException() {
        //Not validating other configuration keys as quarkus already does it for us.
        Assertions.assertThatThrownBy(() -> minioClient.toString())
                .isInstanceOf(ConfigurationException.class)
                .hasMessageStartingWith("\"quarkus.minio.url\" is mandatory");
    }
}
