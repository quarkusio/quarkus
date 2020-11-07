package io.quarkus.minio;

import java.util.function.Predicate;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.minio.MinioClient;
import io.quarkus.runtime.configuration.ConfigurationException;

@Singleton
public class MinioProducer {

    @Inject
    MinioConfiguration configuration;

    private static final Predicate<String> IS_NOT_VALID_MINIO_URL = value -> !value.startsWith("http://")
            || !value.startsWith("https://");

    @Produces
    @ApplicationScoped
    public MinioClient produceMinioClient() {
        verifyUrl();
        try {
            return MinioClient.builder()
                    .endpoint(configuration.url)
                    .credentials(configuration.accessKey, configuration.secretKey)
                    .build();
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
    }

    private void verifyUrl() {
        //Not validating other configuration keys as quarkus already does it for us.
        if (IS_NOT_VALID_MINIO_URL.test(configuration.url)) {
            throw new ConfigurationException("\"quarkus.minio.url\" is mandatory and must be a valid url");
        }
    }
}
