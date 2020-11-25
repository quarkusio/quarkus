package io.quarkus.rest.server.test.resource.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.server.test.resource.basic.resource.MediaTypeFromMessageBodyWriterListAsText;
import io.quarkus.rest.server.test.resource.basic.resource.MediaTypeFromMessageBodyWriterListAsXML;
import io.quarkus.rest.server.test.resource.basic.resource.MediaTypeFromMessageBodyWriterResource;
import io.quarkus.rest.server.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @tpSubChapter Resteasy server
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test that MessageBodyWriters can be consulted for media type
 * @tpSince RESTEasy 3.1.3.Final
 */
@DisplayName("Media Type From Message Body Writer Test")
public class MediaTypeFromMessageBodyWriterTestMultiple {

    @DisplayName("Target")
    private static class Target {

        String path;

        String queryName;

        String queryValue;

        Target(final String path, final String queryName, final String queryValue) {
            this.path = path;
            this.queryName = queryName;
            this.queryValue = queryValue;
        }
    }

    private static String ACCEPT_CHROME = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8";

    private static String ACCEPT_FIREFOX = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";

    private static String ACCEPT_IE11 = "text/html, application/xhtml+xml, */*";

    private static Collection<Target> tgts = new ArrayList<Target>();

    private static Collection<String> accepts = new ArrayList<String>();

    private static Client client;

    static {
        tgts.add(new Target("java.util.TreeSet", null, null));
        tgts.add(new Target("fixed", "type", "text/plain"));
        tgts.add(new Target("fixed", "type", "application/xml"));
        tgts.add(new Target("variants", null, null));
        tgts.add(new Target("variantsObject", null, null));
        accepts.add(ACCEPT_CHROME);
        accepts.add(ACCEPT_FIREFOX);
        accepts.add(ACCEPT_IE11);
        accepts.add("foo/bar,text/plain");
        accepts.add("foo/bar,*/*");
        accepts.add("text/plain");
    }
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class, MediaTypeFromMessageBodyWriterListAsText.class,
                            MediaTypeFromMessageBodyWriterListAsXML.class, MediaTypeFromMessageBodyWriterResource.class);
                    return war;
                }
            });

    @BeforeEach
    public void init() {
        client = ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
    }

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, MediaTypeFromMessageBodyWriterTestMultiple.class.getSimpleName());
    }

    private String generateURLUserCase(String path) {
        return PortProviderUtil.generateURL(path, MediaTypeFromMessageBodyWriterTestMultiple.class.getSimpleName() + "_single");
    }

    /**
     * @tpTestDetails Test that MessageBodyWriters can be consulted for media type
     * @tpSince RESTEasy 3.1.3.Final
     */
    @Test
    @DisplayName("Test")
    public void test() throws Exception {
        WebTarget base = client.target(generateURL(""));
        Response response = null;
        for (Target tgt : tgts) {
            for (String accept : accepts) {
                if (tgt.queryName != null) {
                    response = base.path(tgt.path).queryParam(tgt.queryName, tgt.queryValue).request().header("Accept", accept)
                            .get();
                } else {
                    response = base.path(tgt.path).request().header("Accept", accept).get();
                }
                Assertions.assertEquals(200, response.getStatus());
                String s = response.getHeaderString("X-COUNT");
                Assertions.assertNotNull(s);
                response.close();
            }
        }
        client.close();
    }

}
