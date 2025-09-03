package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.rest.client.reactive.configuration.EchoResource;
import io.quarkus.test.QuarkusUnitTest;

public class MpClassNameScopeOverrideTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(EchoResource.class, MPRestClient.class, HelloClientWithBaseUri.class))
            .withConfigurationResource("mp-classname-scope-test-application.properties");

    @RestClient
    HelloClientWithBaseUri helloClientWithBaseUri;

    @Test
    void shouldHaveApplicationScope() {
        BeanManager beanManager = Arc.container().beanManager();
        Set<Bean<?>> beans = beanManager.getBeans(MPRestClient.class, RestClient.LITERAL);
        Bean<?> resolvedBean = beanManager.resolve(beans);
        assertThat(resolvedBean.getScope()).isEqualTo(ApplicationScoped.class);
    }

    @Test
    void shouldHaveDependentScope() {
        BeanManager beanManager = Arc.container().beanManager();
        Set<Bean<?>> beans = beanManager.getBeans(HelloClientWithBaseUri.class, RestClient.LITERAL);
        Bean<?> resolvedBean = beanManager.resolve(beans);
        assertThat(resolvedBean.getScope()).isEqualTo(Dependent.class);
    }

    @Test
    void shouldConnect() {
        assertThat(helloClientWithBaseUri.echo("Bob")).isEqualTo("hello, Bob!!!");
    }

    @RegisterRestClient(configKey = "mp")
    interface MPRestClient {
        @POST
        @Consumes(MediaType.TEXT_PLAIN)
        @Produces(MediaType.TEXT_PLAIN)
        @Path("/")
        String echo(String name);
    }
}
