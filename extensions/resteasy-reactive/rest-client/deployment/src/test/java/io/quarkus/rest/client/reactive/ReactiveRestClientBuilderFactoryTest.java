package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.configuration.EchoResource;
import io.quarkus.restclient.config.RestClientBuilderFactory;
import io.quarkus.test.QuarkusUnitTest;

public class ReactiveRestClientBuilderFactoryTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(HelloClient2.class, EchoResource.class))
            .withConfigurationResource("factory-test-application.properties");

    @Test
    public void test() throws Exception {
        RestClientBuilder restClientBuilder = RestClientBuilderFactory.getInstance().newBuilder(HelloClient2.class);
        HelloClient2 restClient = restClientBuilder.build(HelloClient2.class);

        assertThat(restClientBuilder.getConfiguration().getProperties().get("io.quarkus.rest.client.read-timeout"))
                .isEqualTo(3456L);
        assertThat(restClient.echo("Hello")).contains("Hello");
    }
}
