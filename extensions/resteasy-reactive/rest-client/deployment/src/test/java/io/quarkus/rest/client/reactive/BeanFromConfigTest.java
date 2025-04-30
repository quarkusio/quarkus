package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;

public class BeanFromConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloClient.class, HelloResource.class))
            .overrideConfigKey("quarkus.rest-client.\"io.quarkus.rest.client.reactive.HelloClient\".scope", "Dependent")
            .overrideRuntimeConfigKey("quarkus.rest-client.\"io.quarkus.rest.client.reactive.HelloClient\".url",
                    "http://localhost:${quarkus.http.test-port:8081}");

    @RestClient
    HelloClient client;

    @Test
    void shouldHello() {
        assertThat(client.echo("w0rld")).isEqualTo("hello, w0rld");
    }

    @Test
    void shouldHaveDependentScope() {
        BeanManager beanManager = Arc.container().beanManager();
        Set<Bean<?>> beans = beanManager.getBeans(HelloClient.class, RestClient.LITERAL);
        Bean<?> resolvedBean = beanManager.resolve(beans);
        assertThat(resolvedBean.getScope()).isEqualTo(Dependent.class);
    }

    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    public static class HelloResource {

        @POST
        public String echo(String name) {
            return "hello, " + name;
        }
    }
}
