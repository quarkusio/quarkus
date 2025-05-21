package io.quarkus.rest.client.reactive.subresource;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import org.jboss.resteasy.reactive.RestQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class SubResourceAndBeanParamTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestClient.class, SubClient.class, TestBean.class, Exception.class, Resource.class));

    @TestHTTPResource
    URI baseUri;

    @Test
    public void test() {
        TestClient testClient = QuarkusRestClientBuilder.newBuilder().baseUri(baseUri).build(TestClient.class);
        assertThat(testClient.subResource(new TestBean("io.quarkus:quarkus-rest")).getExtensionById()).isEqualTo("foo");
    }

    @Path("/api/extensions")
    public static class Resource {

        @GET
        public String getExtensions(@RestQuery String id) {
            return "foo";
        }
    }

    public interface TestClient {

        @Path("/api/extensions")
        SubClient subResource(@BeanParam TestBean bean);
    }

    public interface SubClient {

        @GET
        String getExtensionById();
    }

    public static class TestBean {

        @QueryParam("id")
        public final String extensionName;

        public TestBean(String extensionName) {
            this.extensionName = extensionName;
        }
    }

}
