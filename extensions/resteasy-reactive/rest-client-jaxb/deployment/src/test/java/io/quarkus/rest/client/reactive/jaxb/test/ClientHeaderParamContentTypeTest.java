package io.quarkus.rest.client.reactive.jaxb.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * A {@code Content-Type} set with {@code @ClientHeaderParam} or {@code @Consumes} is sent as is, even when the JAXB
 * writer serializes the body.
 */
public class ClientHeaderParamContentTypeTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(Client.class, EchoResource.class, XmlDto.class))
            .overrideConfigKey("quarkus.rest-client.echo.url", "http://localhost:${quarkus.http.test-port:8081}");

    @RestClient
    Client client;

    @Test
    void overrideIsKept() {
        assertThat(client.sendCsv("a,b")).isEqualTo("text/csv");
    }

    @Test
    void xmlWithParametersIsKept() {
        assertThat(client.sendXmlWithCharset(new XmlDto("foo", "bar"))).isEqualTo("text/xml; charset=ISO-8859-1");
    }

    @Test
    void consumesIsKept() {
        assertThat(client.sendCustomXml(new XmlDto("foo", "bar"))).isEqualTo("application/vnd.acme+xml");
    }

    @Path("/echo")
    @RegisterRestClient(configKey = "echo")
    public interface Client {

        @POST
        @Path("/content-type")
        @Consumes(MediaType.APPLICATION_XML)
        @ClientHeaderParam(name = "Content-Type", value = "text/csv")
        String sendCsv(String body);

        @POST
        @Path("/content-type")
        @Consumes(MediaType.APPLICATION_XML)
        @ClientHeaderParam(name = "Content-Type", value = "text/xml; charset=ISO-8859-1")
        String sendXmlWithCharset(XmlDto dto);

        @POST
        @Path("/content-type")
        @Consumes("application/vnd.acme+xml")
        String sendCustomXml(XmlDto dto);
    }

    @XmlRootElement(name = "dto")
    public static class XmlDto {
        public String name;
        public String value;

        public XmlDto() {
        }

        public XmlDto(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    @Path("/echo")
    public static class EchoResource {

        @POST
        @Path("/content-type")
        @Consumes(MediaType.WILDCARD)
        @Produces(MediaType.TEXT_PLAIN)
        public String contentType(@HeaderParam("Content-Type") String contentType, String body) {
            return contentType;
        }
    }
}
