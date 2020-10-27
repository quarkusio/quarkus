package io.quarkus.rest.server.test.resource.basic;

import java.util.function.Supplier;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.server.test.resource.basic.resource.ApplicationScopeObject;
import io.quarkus.rest.server.test.resource.basic.resource.MultiInterfaceResLocatorIntf1;
import io.quarkus.rest.server.test.resource.basic.resource.MultiInterfaceResLocatorIntf2;
import io.quarkus.rest.server.test.resource.basic.resource.MultiInterfaceResLocatorResource;
import io.quarkus.rest.server.test.resource.basic.resource.MultiInterfaceResLocatorSubresource;
import io.quarkus.rest.server.test.resource.basic.resource.ParameterSubResClassSub;
import io.quarkus.rest.server.test.resource.basic.resource.ParameterSubResConcreteSubImpl;
import io.quarkus.rest.server.test.resource.basic.resource.ParameterSubResDoubleInterface;
import io.quarkus.rest.server.test.resource.basic.resource.ParameterSubResGenericInterface;
import io.quarkus.rest.server.test.resource.basic.resource.ParameterSubResGenericSub;
import io.quarkus.rest.server.test.resource.basic.resource.ParameterSubResInternalInterface;
import io.quarkus.rest.server.test.resource.basic.resource.ParameterSubResRoot;
import io.quarkus.rest.server.test.resource.basic.resource.ParameterSubResRootImpl;
import io.quarkus.rest.server.test.resource.basic.resource.ParameterSubResSub;
import io.quarkus.rest.server.test.resource.basic.resource.ParameterSubResSubImpl;
import io.quarkus.rest.server.test.resource.basic.resource.RequestScopedObject;
import io.quarkus.rest.server.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

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
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
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
        Assertions.assertEquals("resourceCounter:1,appscope:1,requestScope:1", response.readEntity(String.class),
                "Wrong response");
        response = client.target(generateURL("/path/subclass")).request().get();
        Assertions.assertEquals("resourceCounter:2,appscope:2,requestScope:1", response.readEntity(String.class),
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
