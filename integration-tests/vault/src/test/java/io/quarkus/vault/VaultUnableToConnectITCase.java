package io.quarkus.vault;

import static io.quarkus.credentials.CredentialsProvider.PASSWORD_PROPERTY_NAME;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

@Disabled // test is expected to fail on quarkus app startup
public class VaultUnableToConnectITCase {

    // start the test with no vault listening on 4850
    // the application startup should fail with the following debug logs 3 times:
    // attempt ... to fetch secrets from vault failed with: ...
    // retrying to fetch secrets
    // ...
    // you can also validate the retry on read timeout by starting a server that answers
    // POSTs on http://localhost:4850/v1/auth/userpass/login/{user} and sleeps more than 10 secs
    // again you are expected to see several attempts in the logs

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("application-unable-to-connect.properties", "application.properties"));

    @ConfigProperty(name = PASSWORD_PROPERTY_NAME)
    String someSecret;

    @Test
    void test() {
        // we will not reach that point
    }
}
