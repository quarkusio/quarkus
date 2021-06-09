package io.quarkus.rest.client.reactive.registerclientheaders;

import static io.quarkus.rest.client.reactive.RestClientTestUtil.setUrlForClass;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
                                                "org.eclipse.microprofile.rest.client.propagateHeaders="
                                                + HeaderSettingClient.HEADER),
                                "application.properties");
            });

    @RestClient
    EchoClient client;

    @RestClient
    HeaderSettingClient headerSettingClient;

    @Test
    public void shouldUseHeadersFactory() {
        assertEquals("ping:bar", client.echo("ping:"));
    }

    @Test
    public void shouldPassIncomingHeaders() {
        String headerValue = "my-header-value";
        RequestData requestData = headerSettingClient.setHeaderValue(headerValue);
        assertThat(requestData.getHeaders().get(HeaderSettingClient.HEADER).get(0)).isEqualTo(headerValue);
    }

}
