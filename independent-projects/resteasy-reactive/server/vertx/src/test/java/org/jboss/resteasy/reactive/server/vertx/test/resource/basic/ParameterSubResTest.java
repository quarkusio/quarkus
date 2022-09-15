package org.jboss.resteasy.reactive.server.vertx.test.resource.basic;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import java.util.function.Supplier;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.ApplicationScopeObject;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.MultiInterfaceResLocatorIntf1;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.MultiInterfaceResLocatorIntf2;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.MultiInterfaceResLocatorResource;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.MultiInterfaceResLocatorSubresource;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.ParameterSubResClassSub;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.ParameterSubResConcreteSubImpl;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.ParameterSubResDoubleInterface;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.ParameterSubResGenericInterface;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.ParameterSubResGenericSub;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.ParameterSubResInternalInterface;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.ParameterSubResRoot;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.ParameterSubResRootImpl;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.ParameterSubResSub;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.ParameterSubResSubImpl;
import org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource.RequestScopedObject;
import org.jboss.resteasy.reactive.server.vertx.test.simple.PortProviderUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * @tpSubChapter Resources
 * @tpChapter Integration tests
 * @tpTestCaseDetails Test resources with sub-resources with parameters.
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Parameter Sub Res Test")
public class ParameterSubResTest {

    static Client client;

    @RegisterExtension
    static ResteasyReactiveUnitTest testExtension = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClass(MultiInterfaceResLocatorResource.class);
                    war.addClass(MultiInterfaceResLocatorSubresource.class);
                    war.addClasses(MultiInterfaceResLocatorIntf1.class);
                    war.addClasses(MultiInterfaceResLocatorIntf2.class);
                    war.addClass(ParameterSubResConcreteSubImpl.class);
                    war.addClass(ParameterSubResDoubleInterface.class);
                    war.addClass(ParameterSubResGenericInterface.class);
                    war.addClass(ParameterSubResInternalInterface.class);
                    war.addClasses(PortProviderUtil.class);
                    war.addClass(ParameterSubResRoot.class);
                    war.addClass(ParameterSubResClassSub.class);
                    war.addClass(ApplicationScopeObject.class);
                    war.addClass(RequestScopedObject.class);
                    war.addClass(ParameterSubResSub.class);
                    war.addClass(ParameterSubResSubImpl.class);
                    war.addClasses(ParameterSubResRootImpl.class, ParameterSubResGenericSub.class);
                    return war;
                }
            });

    private String generateURL(String path) {
        return PortProviderUtil.generateURL(path, ParameterSubResTest.class.getSimpleName());
    }

    @BeforeEach
    public void init() {
        client = ClientBuilder.newClient();
    }

    @AfterEach
    public void after() throws Exception {
        client.close();
    }

    /**
     * @tpTestDetails Check sub resources.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Sub Resource")
    public void testSubResource() throws Exception {
        Response response = client.target(generateURL("/path/sub/fred")).request().get();
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals("Boo! - fred", response.readEntity(String.class), "Wrong content of response");
    }

    @Test
    @DisplayName("Test Return Sub Resource As Class")
    public void testReturnSubResourceAsClass() throws Exception {
        Response response = client.target(generateURL("/path/subclass")).request().get();
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals("/path/subclass", response.readEntity(String.class),
                "Wrong response");
    }

    /**
     * @tpTestDetails Check root resource.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Root")
    public void testRoot() throws Exception {
        Response response = client.target(generateURL("/generic/sub")).queryParam("foo", "42.0").request().get();
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assertions.assertEquals("42.0", response.readEntity(String.class), "Wrong content of response");
    }
}
