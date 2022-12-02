package io.quarkus.rest.client.reactive.registerclientheaders;

import static io.quarkus.rest.client.reactive.RestClientTestUtil.setUrlForClass;
import static io.quarkus.rest.client.reactive.registerclientheaders.HeaderSettingClient.HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class RegisterClientHeadersTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> {
                return ShrinkWrap.create(JavaArchive.class)
                        .addPackage(EchoClient.class.getPackage())
                        .addAsResource(
                                new StringAsset(
                                        setUrlForClass(EchoClient.class) +
                                                setUrlForClass(HeaderSettingClient.class) +
                                                setUrlForClass(HeaderPassingClient.class) +
                                                setUrlForClass(HeaderNoPassingClient.class) +
                                                setUrlForClass(MultipleHeadersBindingClient.class) +
                                                "org.eclipse.microprofile.rest.client.propagateHeaders=" + HEADER + "\n" +
                                                "header.value=from property file"),
                                "application.properties");
            });

    @RestClient
    EchoClient client;

    @RestClient
    HeaderSettingClient headerSettingClient;

    @RestClient
    MultipleHeadersBindingClient multipleHeadersBindingClient;

    @Test
    public void shouldUseHeadersFactory() {
        assertEquals("ping:bar", client.echo("ping:"));
        assertEquals("nullbar", client.echo(null));
    }

    @Test
    public void shouldPassIncomingHeaders() {
        String headerValue = "my-header-value";
        RequestData requestData = headerSettingClient.setHeaderValue(headerValue);
        assertThat(requestData.getHeaders().get(HEADER).get(0)).isEqualTo(headerValue);
    }

    @Test
    public void shouldNotPassIncomingHeaders() {
        String headerValue = "my-header-value";
        RequestData requestData = headerSettingClient.setHeaderValueNoPassing(headerValue);
        assertThat(requestData.getHeaders().get(HEADER)).isNull();
    }

    @Test
    public void shouldSetHeadersFromMultipleBindings() {
        String headerValue = "my-header-value";
        Map<String, List<String>> headers = multipleHeadersBindingClient.call(headerValue).getHeaders();
        // Verify: @RegisterClientHeaders(MyHeadersFactory.class)
        assertThat(headers.get("foo")).containsExactly("bar");
        // Verify: @ClientHeaderParam(name = "my-header", value = "constant-header-value")
        assertThat(headers.get("my-header")).containsExactly("constant-header-value");
        // Verify: @ClientHeaderParam(name = "computed-header", value = "{...ComputedHeader.get}")
        assertThat(headers.get("computed-header")).containsExactly("From " + ComputedHeader.class.getName());
        // Verify: @ClientHeaderParam(name = "header-from-properties", value = "${header.value}")
        assertThat(headers.get("header-from-properties")).containsExactly("from property file");
        // Verify: @HeaderParam("jaxrs-style-header")
        assertThat(headers.get("jaxrs-style-header")).containsExactly(headerValue);
    }

}
