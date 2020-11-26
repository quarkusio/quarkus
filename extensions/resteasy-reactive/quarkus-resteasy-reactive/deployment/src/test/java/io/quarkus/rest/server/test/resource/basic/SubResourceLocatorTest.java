package io.quarkus.rest.server.test.resource.basic;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.server.test.resource.basic.resource.SubResourceLocatorBaseCrudService;
import io.quarkus.rest.server.test.resource.basic.resource.SubResourceLocatorBaseService;
import io.quarkus.rest.server.test.resource.basic.resource.SubResourceLocatorFoo;
import io.quarkus.rest.server.test.resource.basic.resource.SubResourceLocatorImpFoo;
import io.quarkus.rest.server.test.resource.basic.resource.SubResourceLocatorOhaUserModel;
import io.quarkus.rest.server.test.resource.basic.resource.SubResourceLocatorPlatformServiceImpl;
import io.quarkus.rest.server.test.resource.basic.resource.SubResourceLocatorPlatformServiceResource;
import io.quarkus.rest.server.test.resource.basic.resource.SubResourceLocatorUserResource;
import io.quarkus.rest.server.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

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
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class, SubResourceLocatorBaseCrudService.class,
                            SubResourceLocatorBaseService.class,
                            SubResourceLocatorFoo.class, SubResourceLocatorOhaUserModel.class,
                            SubResourceLocatorPlatformServiceResource.class, SubResourceLocatorUserResource.class);
                    war.addClasses(SubResourceLocatorImpFoo.class, SubResourceLocatorPlatformServiceImpl.class);
                    return war;
                }
            });

    /**
     * @tpTestDetails Sub resource locator should not fail
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test 657")
    public void test657() throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget base = client.target(PortProviderUtil.generateURL("/platform/users/89080/data/ada/jsanchez110",
                SubResourceLocatorTest.class.getSimpleName()));
        Response response = base.request().get();
        String s = response.readEntity(String.class);
        Assertions.assertEquals("bill", s);
        response.close();
        client.close();
    }
}
