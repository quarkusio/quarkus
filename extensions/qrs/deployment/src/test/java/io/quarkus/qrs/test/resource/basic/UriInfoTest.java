package io.quarkus.qrs.test.resource.basic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.category.NotForForwardCompatibility;
import io.quarkus.qrs.test.resource.basic.resource.UriInfoEncodedQueryResource;
import io.quarkus.qrs.test.resource.basic.resource.UriInfoEncodedTemplateResource;
import io.quarkus.qrs.test.resource.basic.resource.UriInfoEscapedMatrParamResource;
import io.quarkus.qrs.test.resource.basic.resource.UriInfoQueryParamsResource;
import io.quarkus.qrs.test.resource.basic.resource.UriInfoRelativizeResource;
import io.quarkus.qrs.test.resource.basic.resource.UriInfoSimpleResource;
import io.quarkus.qrs.test.resource.basic.resource.UriInfoSimpleSingletonResource;
import io.quarkus.qrs.runtime.util.HttpResponseCodes;
import io.quarkus.qrs.test.PermissionUtil;
import io.quarkus.qrs.test.PortProviderUtil;
import io.quarkus.qrs.test.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.quarkus.test.QuarkusUnitTest;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.PropertyPermission;
import org.junit.jupiter.api.DisplayName;
import java.util.function.Supplier;
import org.jboss.shrinkwrap.api.ShrinkWrap;

/**
 * @tpSubChapter Resources
 * @tpChapter Integration tests
 * @tpTestCaseDetails Tests for java.net.URI class
 * @tpSince RESTEasy 3.0.16
 */
@DisplayName("Uri Info Test")
public class UriInfoTest {

    protected final Logger logger = LogManager.getLogger(UriInfoTest.class.getName());

    private static Client client;

    @BeforeAll
    public static void before() throws Exception {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void after() throws Exception {
        client.close();
        client = null;
    }

    @SuppressWarnings(value = "unchecked")
    @Deployment(name = "UriInfoSimpleResource")
    public static Archive<?> deployUriInfoSimpleResource() {
        JavaArchive war = ShrinkWrap.create(JavaArchive.class);
        war.addClass(PortProviderUtil.class);
        // Use of PortProviderUtil in the deployment
        war.addAsManifestResource(PermissionUtil.createPermissionsXmlAsset(new PropertyPermission("node", "read"), new PropertyPermission("ipv6", "read"), new RuntimePermission("getenv.RESTEASY_PORT"), new PropertyPermission("org.jboss.resteasy.port", "read")), "permissions.xml");
        war.addClasses(UriInfoSimpleResource.class); return war; }});

    /**
     * @tpTestDetails Check uri from resource on server. Simple resource is used.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Uri Info")
    public void testUriInfo() throws Exception {
        basicTest("/simple", UriInfoSimpleResource.class.getSimpleName());
        basicTest("/simple/fromField", UriInfoSimpleResource.class.getSimpleName());
    }

    @Deployment(name = "UriInfoSimpleSingletonResource")
    public static Archive<?> deployUriInfoSimpleResourceAsSingleton() {
        JavaArchive war = ShrinkWrap.create(JavaArchive.class);
        war.addClass(PortProviderUtil.class);
        war.addAsManifestResource(PermissionUtil.createPermissionsXmlAsset(new PropertyPermission("node", "read"), new PropertyPermission("ipv6", "read"), new RuntimePermission("getenv.RESTEASY_PORT"), new PropertyPermission("org.jboss.resteasy.port", "read")), "permissions.xml");
        List<Class<?>> singletons = new ArrayList<>();
        singletons.add(UriInfoSimpleSingletonResource.class);
        war.addClasses(singletons, (Class<?>[]) null); return war; }});

    /**
     * @tpTestDetails Check uri from resource on server. Resource is set as singleton to RESTEasy.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Uri Info With Singleton")
    public void testUriInfoWithSingleton() throws Exception {
        basicTest("/simple/fromField", UriInfoSimpleSingletonResource.class.getSimpleName());
    }

    @Deployment(name = "UriInfoEscapedMatrParamResource")
    public static Archive<?> deployUriInfoEscapedMatrParamResource() {
        JavaArchive war = ShrinkWrap.create(JavaArchive.class);
        war.addClass(PortProviderUtil.class);
        war.addClasses(UriInfoEscapedMatrParamResource.class); return war; }});

    /**
     * @tpTestDetails Check uri from resource on server. Test complex parameter.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Escaped Matr Param")
    public void testEscapedMatrParam() throws Exception {
        basicTest("/queryEscapedMatrParam;a=a%3Bb;b=x%2Fy;c=m%5Cn;d=k%3Dl", UriInfoEscapedMatrParamResource.class.getSimpleName());
    }

    @Deployment(name = "UriInfoEncodedTemplateResource")
    public static Archive<?> deployUriInfoEncodedTemplateResource() {
        JavaArchive war = ShrinkWrap.create(JavaArchive.class);
        war.addClass(PortProviderUtil.class);
        war.addClasses(UriInfoEncodedTemplateResource.class); return war; }});

    /**
     * @tpTestDetails Check uri from resource on server. Test space character in URI.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Encoded Template Params")
    public void testEncodedTemplateParams() throws Exception {
        basicTest("/a%20b/x%20y", UriInfoEncodedTemplateResource.class.getSimpleName());
    }

    @Deployment(name = "UriInfoEncodedQueryResource")
    public static Archive<?> deployUriInfoEncodedQueryResource() {
        JavaArchive war = ShrinkWrap.create(JavaArchive.class);
        war.addClass(PortProviderUtil.class);
        war.addClasses(UriInfoEncodedQueryResource.class); return war; }});

    /**
     * @tpTestDetails Check uri from resource on server. Test space character in URI attribute.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Encoded Query Params")
    public void testEncodedQueryParams() throws Exception {
        basicTest("/query?a=a%20b", UriInfoEncodedQueryResource.class.getSimpleName());
    }

    @Deployment(name = "UriInfoRelativizeResource")
    public static Archive<?> deployUriInfoRelativizeResource() {
        JavaArchive war = ShrinkWrap.create(JavaArchive.class);
        war.addClass(PortProviderUtil.class);
        war.addClasses(UriInfoRelativizeResource.class); return war; }});

    /**
     * @tpTestDetails Check uri from resource on server. Test return value from resource - same URI address.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Relativize")
    public void testRelativize() throws Exception {
        String uri = PortProviderUtil.generateURL("/", UriInfoRelativizeResource.class.getSimpleName());
        WebTarget target = client.target(uri);
        String result;
        result = target.path("a/b/c").queryParam("to", "a/d/e").request().get(String.class);
        Assertions.assertEquals(result, "../../d/e");
        result = target.path("a/b/c").queryParam("to", UriBuilder.fromUri(uri).path("a/d/e").build().toString()).request().get(String.class);
        Assertions.assertEquals(result, "../../d/e");
        result = target.path("a/b/c").queryParam("to", "http://foobar/a/d/e").request().get(String.class);
        Assertions.assertEquals(result, "http://foobar/a/d/e");
    }

    /**
     * @tpTestDetails Check uri on client. Base unit test.
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    @DisplayName("Test Resolve")
    public void testResolve() throws Exception {
        URI uri = new URI("http://localhost/base1/base2");
        logger.info(String.format("Resolved foo: %s", uri.resolve("foo")));
        logger.info(String.format("Resolved /foo: %s", uri.resolve("/foo")));
        logger.info(String.format("Resolved ../foo: %s", uri.resolve("../foo")));
    }

    private void basicTest(String path, String testName) throws Exception {
        Response response = client.target(PortProviderUtil.generateURL(path, testName)).request().get();
        try {
            Assertions.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
        } finally {
            response.close();
        }
    }

    @Deployment(name = "UriInfoQueryParamsResource")
    public static Archive<?> deployUriInfoQueryParamsResource() {
        JavaArchive war = ShrinkWrap.create(JavaArchive.class);
        war.addClass(PortProviderUtil.class);
        war.addClasses(UriInfoQueryParamsResource.class); return war; }});

    /**
     * @tpTestDetails Test that UriInfo.getQueryParameters() returns an immutable map. Test's logic is in end-point.
     * @tpSince RESTEasy 3.0.17
     */
    @Test

    @DisplayName("Test Query Params Mutability")
    public void testQueryParamsMutability() throws Exception {
        basicTest("/queryParams?a=a,b", "UriInfoQueryParamsResource");
    }
}
