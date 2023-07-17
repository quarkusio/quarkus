package io.quarkus.rest.client.reactive.beanparam;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class BeanPathParamTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest();

    @TestHTTPResource
    URI baseUri;

    @Test
    void shouldPassPathParamFromBeanParam() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        assertThat(client.getWithBeanParam(new MyBeanParam("123"))).isEqualTo("it works!");
    }

    @Test
    void shouldPassPathParamFromBeanParamAndMethod() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        assertThat(client.getWithBeanParam("foo", new MyBeanParam("123"))).isEqualTo("it works with method too!");
    }

    @Test
    void shouldResolvePathParamsWhenBeanParamClassExtendsAnother() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
        assertThat(client.getWithBeanParamInheritance(new MyChildBeanParam("child", "123"))).isEqualTo(
                "it works with inheritance too!");
    }

    @Path("/my/{id}/resource")
    public interface Client {
        @GET
        String getWithBeanParam(@BeanParam MyBeanParam beanParam);

        @GET
        @Path("/{name}")
        String getWithBeanParam(@PathParam("name") String name, @BeanParam MyBeanParam beanParam);

        @GET
        @Path("/item/{base}")
        String getWithBeanParamInheritance(@BeanParam MyChildBeanParam beanParam);
    }

    public static class MyBeanParam {
        private final String id;

        public MyBeanParam(String id) {
            this.id = id;
        }

        @PathParam("id")
        public String getId() {
            return id;
        }
    }

    public static class MyChildBeanParam extends MyBeanParam {
        private final String base;

        public MyChildBeanParam(String base, String id) {
            super(id);
            this.base = base;
        }

        @PathParam("base")
        public String getBase() {
            return base;
        }
    }

    public static class TestMyChildBeanParam {
        private final String id;
        private final String base;

        public TestMyChildBeanParam(String base, String id) {
            this.id = id;
            this.base = base;
        }

        @PathParam("id")
        public String getId() {
            return id;
        }

        @PathParam("base")
        public String getBase() {
            return base;
        }
    }

    @Path("/my/123/resource")
    public static class Resource {
        @GET
        public String get() {
            return "it works!";
        }

        @Path("/foo")
        @GET
        public String getWithLongerPath() {
            return "it works with method too!";
        }

        @Path("/item/child")
        @GET
        public String getWithInheritance() {
            return "it works with inheritance too!";
        }
    }
}
