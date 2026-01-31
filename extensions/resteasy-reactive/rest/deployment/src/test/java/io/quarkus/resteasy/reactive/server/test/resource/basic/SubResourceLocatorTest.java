package io.quarkus.resteasy.reactive.server.test.resource.basic;

import java.net.URI;
import java.util.function.Supplier;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.SubResourceLocatorBaseCrudService;
import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.SubResourceLocatorBaseService;
import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.SubResourceLocatorFoo;
import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.SubResourceLocatorImpFoo;
import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.SubResourceLocatorOhaUserModel;
import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.SubResourceLocatorPlatformServiceImpl;
import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.SubResourceLocatorPlatformServiceResource;
import io.quarkus.resteasy.reactive.server.test.resource.basic.resource.SubResourceLocatorUserResource;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

/**
 * @tpSubChapter Resources
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-657
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Sub Resource Locator Test")
public class SubResourceLocatorTest {

    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(SubResourceLocatorBaseCrudService.class,
                            SubResourceLocatorBaseService.class,
                            SubResourceLocatorFoo.class, SubResourceLocatorOhaUserModel.class,
                            SubResourceLocatorPlatformServiceResource.class, SubResourceLocatorUserResource.class);
                    war.addClasses(SubResourceLocatorImpFoo.class, SubResourceLocatorPlatformServiceImpl.class);
                    return war;
                }
            });

    @TestHTTPResource
    URI uri;

    /**
     * @tpTestDetails Sub resource locator should not fail
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test 657")
    public void test657() {
        Client client = ClientBuilder.newClient();
        WebTarget base = client.target(UriBuilder.fromUri(uri).path("/platform/users/89080/data/ada/jsanchez110"));
        Response response = base.request().get();
        String s = response.readEntity(String.class);
        Assertions.assertEquals("bill", s);
        response.close();
        client.close();
    }
}
