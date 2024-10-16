package io.quarkus.restclient.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.restclient.config.RestClientBuilderFactory;
import io.quarkus.test.QuarkusUnitTest;

public class ClassicRestClientBuilderFactoryTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(EchoClientWithoutAnnotation.class, EchoClientWithConfigKey.class,
                    EchoClientWithEmptyPath.class, EchoResource.class))
            .withConfigurationResource("factory-test-application.properties");

    @Test
    public void testAnnotatedClientClass() {
        RestClientBuilder restClientBuilder = RestClientBuilderFactory.getInstance().newBuilder(EchoClientWithConfigKey.class);
        EchoClientWithConfigKey restClient = restClientBuilder.build(EchoClientWithConfigKey.class);

        assertThat(restClient.echo("Hello")).contains("Hello");
    }

    @Test
    public void testNotAnnotatedClientClass() {
        RestClientBuilder restClientBuilder = RestClientBuilderFactory.getInstance()
                .newBuilder(EchoClientWithoutAnnotation.class);
        EchoClientWithoutAnnotation restClient = restClientBuilder.build(EchoClientWithoutAnnotation.class);

        assertThat(restClient.echo("Hello")).contains("Hello");
    }

    @Test
    public void testEmptyPathAnnotationOnClass() {
        RestClientBuilder restClientBuilder = RestClientBuilderFactory.getInstance()
                .newBuilder(EchoClientWithEmptyPath.class);
        EchoClientWithEmptyPath restClient = restClientBuilder.build(EchoClientWithEmptyPath.class);

        assertThat(restClient.echo("echo", "Hello")).contains("Hello");
    }
}
