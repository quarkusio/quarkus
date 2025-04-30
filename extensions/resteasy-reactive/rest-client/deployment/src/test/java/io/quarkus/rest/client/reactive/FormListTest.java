package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.resteasy.reactive.RestForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class FormListTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(VoidReturnTypeTest.Resource.class));

    @TestHTTPResource
    URI baseUri;

    @Test
    void test() {
        Client client = RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);

        Holder holder = new Holder();
        holder.input2 = List.of("1", "2");
        assertThat(client.call(List.of("first", "second", "third"), holder)).isEqualTo("first-second-third/1-2");
        assertThat(client.call(List.of("first"), holder)).isEqualTo("first/1-2");
    }

    @Path("/test")
    public static class Resource {

        @POST
        public String response(@RestForm List<String> input, @RestForm List<String> input2) {
            return String.join("-", input) + "/" + String.join("-", input2);
        }
    }

    @Path("/test")
    public interface Client {

        @POST
        String call(@RestForm List<String> input, @BeanParam Holder holder);
    }

    public static class Holder {
        @RestForm
        public List<String> input2;
    }
}
