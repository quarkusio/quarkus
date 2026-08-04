package io.quarkus.rest.client.reactive.beanparam;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.MatrixParam;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestMatrix;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class BeanMatrixParamTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(Resource.class, Client.class, MatrixBean.class));

    @TestHTTPResource
    URI baseUri;

    @Test
    void clientSendsMatrixParamsFromBeanParam() {
        Client client = QuarkusRestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        assertThat(client.greet(new MatrixBean("world", "en"))).isEqualTo("hello world/en");
    }

    @Path("/greet")
    public static class Resource {

        @GET
        public String greet(@MatrixParam("name") String name, @MatrixParam("lang") String lang) {
            return "hello " + name + "/" + lang;
        }
    }

    @Path("/greet")
    public interface Client {

        @GET
        String greet(@BeanParam MatrixBean bean);
    }

    public static class MatrixBean {
        @MatrixParam("name")
        public String name;

        @RestMatrix
        public String lang;

        public MatrixBean(String name, String lang) {
            this.name = name;
            this.lang = lang;
        }
    }
}
