package org.jboss.resteasy.reactive.server.vertx.test.resource.basic;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.util.function.Supplier;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.SubResourceLocatorBaseCrudService;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.SubResourceLocatorBaseService;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.SubResourceLocatorFoo;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.SubResourceLocatorImpFoo;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.SubResourceLocatorOhaUserModel;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.SubResourceLocatorPlatformServiceImpl;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.SubResourceLocatorPlatformServiceResource;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.SubResourceLocatorUserResource;
import org.jboss.resteasy.reactive.server.vertx.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * @tpSubChapter Resources
 * @tpChapter Integration tests
 * @tpTestCaseDetails Regression test for RESTEASY-657
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Sub Resource Locator Test")
public class SubResourceLocatorTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest testExtension = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
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
