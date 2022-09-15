package io.quarkus.rest.client.reactive.jaxb.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Objects;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class SimpleJaxbTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withEmptyApplication();

    @TestHTTPResource
    URI uri;

    @Test
    void shouldConsumeXMLEntity() {
        var dto = RestClientBuilder.newBuilder().baseUri(uri).build(XmlClient.class)
                .dto();
        assertThat(dto).isEqualTo(new Dto("foo", "bar"));
    }

    @Test
    void shouldConsumePlainXMLEntity() {
        var dto = RestClientBuilder.newBuilder().baseUri(uri).build(XmlClient.class)
                .plain();
        assertThat(dto).isEqualTo(new Dto("foo", "bar"));
    }

    @Path("/xml")
    public interface XmlClient {

        @GET
        @Path("/dto")
        @Produces(MediaType.APPLICATION_XML)
        Dto dto();

        @GET
        @Path("/plain")
        @Produces(MediaType.TEXT_XML)
        Dto plain();
    }

    @Path("/xml")
    public static class XmlResource {

        private static final String DTO_FOO_BAR = "<?xml version=\"1.0\" encoding=\"UTF-8\" "
                + "standalone=\"yes\"?><Dto><name>foo</name><value>bar</value></Dto>";

        @GET
        @Produces(MediaType.APPLICATION_XML)
        @Path("/dto")
        public String dto() {
            return DTO_FOO_BAR;
        }

        @GET
        @Produces(MediaType.TEXT_XML)
        @Path("/plain")
        public String plain() {
            return DTO_FOO_BAR;
        }
    }

    @XmlRootElement(name = "Dto")
    public static class Dto {
        public String name;
        public String value;

        public Dto(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public Dto() {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Dto dto = (Dto) o;
            return Objects.equals(name, dto.name) && Objects.equals(value, dto.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value);
        }
    }
}
