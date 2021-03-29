package io.quarkus.tck.restclient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import javax.inject.Inject;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.eclipse.microprofile.rest.client.tck.InvokeWithJsonBProviderTest;
import org.eclipse.microprofile.rest.client.tck.WiremockArquillianTest;
import org.eclipse.microprofile.rest.client.tck.interfaces.JsonBClient;
import org.eclipse.microprofile.rest.client.tck.interfaces.MyJsonBObject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * copied from
 * https://github.com/eclipse/microprofile-rest-client/blob/1.4.X-service/tck/src/main/java/org/eclipse/microprofile/rest/client/tck/InvokeWithJsonBProviderTest.java
 */
public class CustomInvokeWithJsonBProviderTest extends WiremockArquillianTest {

    private static final String CDI = "cdi";
    private static final String BUILT = "built";

    @Deployment
    public static WebArchive createDeployment() {
        StringAsset mpConfig = new StringAsset(JsonBClient.class.getName() + "/mp-rest/uri=" + getStringURL());
        return ShrinkWrap.create(WebArchive.class, InvokeWithJsonBProviderTest.class.getSimpleName() + ".war")
                .addClasses(JsonBClient.class, WiremockArquillianTest.class, MyJsonBObject.class,
                        InvokeWithJsonBProviderTest.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsWebInfResource(mpConfig, "classes/META-INF/microprofile-config.properties");
    }

    private static void assumeJsonbApiExists() throws SkipException {
        try {
            Class.forName("javax.json.bind.annotation.JsonbProperty");
        } catch (Throwable t) {
            throw new SkipException("Skipping since JSON-B APIs were not found.");
        }
    }

    @RestClient
    @Inject
    private JsonBClient cdiJsonBClient;

    private JsonBClient builtJsonBClient;

    public void setupClient() {
        builtJsonBClient = RestClientBuilder.newBuilder()
                .baseUri(getServerURI())
                .build(JsonBClient.class);
    }

    @Test
    public void testGetExecutesForBothClients() throws Exception {
        setupClient();
        assumeJsonbApiExists();
        testGet(builtJsonBClient, BUILT);
        testGet(cdiJsonBClient, CDI);
    }

    private void testGet(JsonBClient client, String clientType) {
        reset();
        stubFor(get(urlEqualTo("/myObject"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{" +
                                "\"objectName\": \"myObject\"," +
                                "\"quantity\": 17," +
                                "\"date\": \"2018-12-04\"" +
                                "}")));

        MyJsonBObject obj = client.get("myObject");
        assertEquals(obj.getName(), "myObject");
        assertEquals(obj.getQty(), 17);
        assertEquals(obj.getIgnoredField(), "CTOR");
        assertEquals(obj.getDate(), LocalDate.of(2018, 12, 4));
    }
}
