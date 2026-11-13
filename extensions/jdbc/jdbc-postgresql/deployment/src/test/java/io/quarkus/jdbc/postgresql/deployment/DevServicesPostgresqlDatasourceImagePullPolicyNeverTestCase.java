package io.quarkus.jdbc.postgresql.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class DevServicesPostgresqlDatasourceImagePullPolicyNeverTestCase {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.datasource.db-kind", "postgresql")
            // this tag exists neither locally nor in the registry
            .overrideConfigKey("quarkus.datasource.devservices.image-name", "postgres:quarkus-image-pull-policy-never")
            .overrideConfigKey("quarkus.datasource.devservices.image-pull-policy", "never")
            // the image is not pulled, so the container cannot be created;
            // with any other policy, the build would fail earlier, when pulling the image
            .assertException(t -> assertThat(t)
                    .hasStackTraceContaining("No such image: postgres:quarkus-image-pull-policy-never"));

    @Test
    public void testImageIsNotPulled() {
        // the assertion is done in assertException
    }
}
