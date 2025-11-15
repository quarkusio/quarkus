package io.quarkus.resteasy.test.security.inheritance;

import static io.quarkus.resteasy.test.security.inheritance.SecurityAnnotation.PATH_SEPARATOR;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_PATH_ON_INTERFACE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_PATH_ON_PARENT_RESOURCE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_PATH_ON_RESOURCE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_SECURITY_ON_BASE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_SECURITY_ON_INTERFACE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_SECURITY_ON_PARENT;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_METHOD_WITH_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_ON_BASE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_ON_INTERFACE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_ON_PARENT;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.PARENT_METHOD_WITH_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.SECURED_SUB_RESOURCE_ENDPOINT_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.SUB_DECLARED_ON_BASE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.SUB_DECLARED_ON_INTERFACE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.SUB_DECLARED_ON_PARENT;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.SUB_IMPL_ON_BASE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.SUB_IMPL_ON_INTERFACE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.SUB_IMPL_ON_PARENT;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * Tests that implementation method is always secured when a standard security annotation is on a class
 * or on a class method or when additional method security (like the default JAX-RS security) is in place.
 */
public abstract class AbstractImplMethodSecuredTest {

    protected static QuarkusUnitTest getRunner() {
        return getRunner("");
    }

    protected static QuarkusUnitTest getRunner(String applicationProperties) {
        return new QuarkusUnitTest()
                .withApplicationRoot((jar) -> jar
                        .addPackage("io.quarkus.resteasy.test.security.inheritance.noclassannotation")
                        .addPackage("io.quarkus.resteasy.test.security.inheritance.classrolesallowed")
                        .addPackage("io.quarkus.resteasy.test.security.inheritance.classdenyall")
                        .addPackage("io.quarkus.resteasy.test.security.inheritance.classpermitall")
                        .addClasses(TestIdentityProvider.class, TestIdentityController.class, SecurityAnnotation.class,
                                SubPaths.class)
                        .addAsResource(new StringAsset(applicationProperties + System.lineSeparator()),
                                "application.properties"));
    }

    @BeforeAll
    public static void setupUsers() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin")
                .add("user", "user", "user");
    }

    protected boolean denyAllUnannotated() {
        return false;
    }

    protected String roleRequiredForUnannotatedEndpoint() {
        return null;
    }

    private void assertPath(String basePath, Object securityAnnotationObj, String classSecurityOn) {
        assertPath(basePath, toSecurityAnnotation(securityAnnotationObj), classSecurityOn);
    }

    private void assertSecuredSubResourcePath(String basePath) {

        // sub resource locator is not secured, e.g. @Path("sub") public SubResource subResource() { ... }
        var path = SecurityAnnotation.NONE.assemblePath(basePath) + SECURED_SUB_RESOURCE_ENDPOINT_PATH;
        var methodSubPath = SecurityAnnotation.NONE.methodSubPath(basePath) + SECURED_SUB_RESOURCE_ENDPOINT_PATH;

        // sub resource endpoint itself has RolesAllowed, e.g. @RolesAllowed @Path("endpoint") String endpoint() { ... }
        var securityAnnotation = SecurityAnnotation.METHOD_ROLES_ALLOWED;

        assertPath(path, methodSubPath, securityAnnotation);
    }

    private void assertPath(String basePath, SecurityAnnotation securityAnnotation, String classSecurityOn) {
        var path = securityAnnotation.assemblePath(basePath, classSecurityOn);
        var methodSubPath = securityAnnotation.methodSubPath(basePath, classSecurityOn);
        assertPath(path, methodSubPath, securityAnnotation);
    }

    private void assertPath(String path, String methodSubPath, SecurityAnnotation securityAnnotation) {
        var invalidPayload = "}{\"simple\": \"obj\"}";
        var validPayload = "{\"simple\": \"obj\"}";

        boolean defJaxRsSecurity = denyAllUnannotated() || roleRequiredForUnannotatedEndpoint() != null;
        boolean endpointSecuredWithDefJaxRsSec = defJaxRsSecurity && !securityAnnotation.hasSecurityAnnotation();
        boolean endpointSecured = endpointSecuredWithDefJaxRsSec || securityAnnotation.endpointSecured();

        // test anonymous - for secured endpoints: unauthenticated
        if (endpointSecured) {
            given().contentType(ContentType.JSON).body(invalidPayload).post(path).then().statusCode(401);
        } else {
            given().contentType(ContentType.JSON).body(validPayload).post(path).then().statusCode(200).body(is(methodSubPath));
        }

        // test user - for secured endpoints: unauthorized
        if (endpointSecured) {
            given().contentType(ContentType.JSON).body(invalidPayload).auth().preemptive().basic("user", "user").post(path)
                    .then().statusCode(403);
        } else {
            given().contentType(ContentType.JSON).body(validPayload).auth().preemptive().basic("user", "user").post(path).then()
                    .statusCode(200).body(is(methodSubPath));
        }

        // test admin - for secured endpoints: authorized
        boolean denyAccess = securityAnnotation.denyAll() || (endpointSecuredWithDefJaxRsSec && denyAllUnannotated());
        if (denyAccess) {
            given().contentType(ContentType.JSON).body(invalidPayload).auth().preemptive().basic("admin", "admin").post(path)
                    .then().statusCode(403);
        } else {
            given().contentType(ContentType.JSON).body(invalidPayload).auth().preemptive().basic("admin", "admin").post(path)
                    .then().statusCode(400);
            given().contentType(ContentType.JSON).body(validPayload).auth().preemptive().basic("admin", "admin").post(path)
                    .then().statusCode(200).body(is(methodSubPath));
        }
    }

    private static void assertNotFound(String basePath) {
        var path = SecurityAnnotation.NONE.assembleNotFoundPath(basePath);
        // this assures that not-tested scenarios are simply not supported by RESTEasy
        // should this assertion fail, we need to assure implementation method is secured
        given().contentType(ContentType.JSON).body("{\"simple\": \"obj\"}").post(path).then().statusCode(404);
    }

    private static SecurityAnnotation toSecurityAnnotation(Object securityAnnotationObj) {
        // we use Object due to @EnumSource class loading problems
        return SecurityAnnotation.valueOf(securityAnnotationObj.toString());
    }

    @Test
    public void test_ClassPathOnInterface_ImplOnBaseResource_ImplMetWithPath() {
        var resourceSubPath = CLASS_PATH_ON_INTERFACE + PATH_SEPARATOR + CLASS_PATH_ON_INTERFACE + IMPL_ON_BASE
                + IMPL_METHOD_WITH_PATH;
        assertNotFound(resourceSubPath);
    }

    @EnumSource(SecurityAnnotation.class)
    @ParameterizedTest
    public void test_ClassPathOnResource_ImplOnBaseResource_ImplMetWithPath(Object securityAnnotationObj) {
        var resourceSubPath = CLASS_PATH_ON_RESOURCE + PATH_SEPARATOR + CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE
                + IMPL_METHOD_WITH_PATH;
        assertPath(resourceSubPath, securityAnnotationObj, CLASS_SECURITY_ON_BASE);
    }

    @Test
    public void test_ClassPathOnParentResource_ImplOnBaseResource_ImplMetWithPath() {
        var resourceSubPath = CLASS_PATH_ON_PARENT_RESOURCE + PATH_SEPARATOR + CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE
                + IMPL_METHOD_WITH_PATH;
        assertNotFound(resourceSubPath);
    }

    @EnumSource(SecurityAnnotation.class)
    @ParameterizedTest
    public void test_ClassPathOnInterface_ImplOnBaseResource_InterfaceMetWithPath(Object securityAnnotationObj) {
        var resourceSubPath = CLASS_PATH_ON_INTERFACE + PATH_SEPARATOR + CLASS_PATH_ON_INTERFACE + IMPL_ON_BASE
                + INTERFACE_METHOD_WITH_PATH;
        assertPath(resourceSubPath, securityAnnotationObj, CLASS_SECURITY_ON_BASE);
    }

    @EnumSource(SecurityAnnotation.class)
    @ParameterizedTest
    public void test_ClassPathOnResource_ImplOnBaseResource_InterfaceMetWithPath(Object securityAnnotationObj) {
        var resourceSubPath = CLASS_PATH_ON_RESOURCE + PATH_SEPARATOR + CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE
                + INTERFACE_METHOD_WITH_PATH;
        assertPath(resourceSubPath, securityAnnotationObj, CLASS_SECURITY_ON_BASE);
    }

    @EnumSource(SecurityAnnotation.class)
    @ParameterizedTest
    public void test_ClassPathOnParentResource_ImplOnBaseResource_InterfaceMetWithPath(Object securityAnnotationObj) {
        var resourceSubPath = CLASS_PATH_ON_PARENT_RESOURCE + PATH_SEPARATOR + CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE
                + INTERFACE_METHOD_WITH_PATH;
        assertPath(resourceSubPath, securityAnnotationObj, CLASS_SECURITY_ON_BASE);
    }

    @Test
    public void test_ClassPathOnInterface_ImplOnBaseResource_ParentMetWithPath() {
        var resourceSubPath = CLASS_PATH_ON_INTERFACE + PATH_SEPARATOR + CLASS_PATH_ON_INTERFACE + IMPL_ON_BASE
                + PARENT_METHOD_WITH_PATH;
        assertNotFound(resourceSubPath);
    }

    @EnumSource(SecurityAnnotation.class)
    @ParameterizedTest
    public void test_ClassPathOnResource_ImplOnBaseResource_ParentMetWithPath(Object securityAnnotationObj) {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        var resourceSubPath = CLASS_PATH_ON_RESOURCE + PATH_SEPARATOR + CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE
                + PARENT_METHOD_WITH_PATH;
        assertPath(resourceSubPath, securityAnnotationObj, CLASS_SECURITY_ON_BASE);
    }

    @EnumSource(SecurityAnnotation.class)
    @ParameterizedTest
    public void test_ClassPathOnParentResource_ImplOnBaseResource_ParentMetWithPath(Object securityAnnotationObj) {
        var resourceSubPath = CLASS_PATH_ON_PARENT_RESOURCE + PATH_SEPARATOR + CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE
                + PARENT_METHOD_WITH_PATH;
        assertPath(resourceSubPath, securityAnnotationObj, CLASS_SECURITY_ON_BASE);
    }

    @Disabled("RESTEasy doesn't support JAX-RS post-match filters or CDI interceptors for default interface methods")
    @EnumSource(SecurityAnnotation.class)
    @ParameterizedTest
    public void test_ClassPathOnInterface_SubDeclaredOnInterface_SubImplOnInterface(Object securityAnnotationObj) {
        // this combination won't work in the RESTEasy Classic because it looks like this:
        // @Path("i")
        // public interface I {
        //    @Path("sub")
        //    @RolesAllowed("admin")
        //    default SubResource subResource() {
        //      return null;
        //    }
        // }
        // but post-match JAX-RS filter is not invoked for SubResource locator
        // and CDI interceptors doesn't work for default interface methods

        var resourceSubPath = CLASS_PATH_ON_INTERFACE + PATH_SEPARATOR + CLASS_PATH_ON_INTERFACE
                + SUB_DECLARED_ON_INTERFACE + SUB_IMPL_ON_INTERFACE;
        assertPath(resourceSubPath, securityAnnotationObj, CLASS_SECURITY_ON_INTERFACE);
    }

    @Test
    public void test_ClassPathOnInterface_SubDeclaredOnInterface_SubImplOnBase_SecurityInsideSub() {
        // HINT: test security is inside sub resource on an endpoint method
        var resourceSubPath = CLASS_PATH_ON_INTERFACE + PATH_SEPARATOR + CLASS_PATH_ON_INTERFACE
                + SUB_DECLARED_ON_INTERFACE + SUB_IMPL_ON_BASE;
        assertSecuredSubResourcePath(resourceSubPath);
    }

    @EnumSource(SecurityAnnotation.class)
    @ParameterizedTest
    public void test_ClassPathOnInterface_SubDeclaredOnInterface_SubImplOnParent(Object securityAnnotationObj) {
        // HINT: test security for '@Path("sub") SubResource subResource' but not inside endpoints 'SubResource' itself
        var resourceSubPath = CLASS_PATH_ON_INTERFACE + PATH_SEPARATOR + CLASS_PATH_ON_INTERFACE
                + SUB_DECLARED_ON_INTERFACE + SUB_IMPL_ON_PARENT;
        assertPath(resourceSubPath, securityAnnotationObj, CLASS_SECURITY_ON_PARENT);
    }

    @EnumSource(SecurityAnnotation.class)
    @ParameterizedTest
    public void test_ClassPathOnResource_SubDeclaredOnBase_SubImplOnBase(Object securityAnnotationObj) {
        // HINT: test security for '@Path("sub") SubResource subResource' but not inside endpoints 'SubResource' itself
        var resourceSubPath = CLASS_PATH_ON_RESOURCE + PATH_SEPARATOR + CLASS_PATH_ON_RESOURCE
                + SUB_DECLARED_ON_BASE + SUB_IMPL_ON_BASE;
        assertPath(resourceSubPath, securityAnnotationObj, CLASS_SECURITY_ON_BASE);
    }

    @EnumSource(SecurityAnnotation.class)
    @ParameterizedTest
    public void test_ClassPathOnParentResource_SubDeclaredOnParent_SubImplOnParent(Object securityAnnotationObj) {
        // HINT: test security for '@Path("sub") SubResource subResource' but not inside endpoints 'SubResource' itself
        var resourceSubPath = CLASS_PATH_ON_PARENT_RESOURCE + PATH_SEPARATOR + CLASS_PATH_ON_PARENT_RESOURCE
                + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_PARENT;
        assertPath(resourceSubPath, securityAnnotationObj, CLASS_SECURITY_ON_PARENT);
    }

    @EnumSource(SecurityAnnotation.class)
    @ParameterizedTest
    public void test_ClassPathOnParentResource_SubDeclaredOnParent_SubImplOnBase(Object securityAnnotationObj) {
        // HINT: test security for '@Path("sub") SubResource subResource' but not inside endpoints 'SubResource' itself
        var resourceSubPath = CLASS_PATH_ON_PARENT_RESOURCE + PATH_SEPARATOR + CLASS_PATH_ON_PARENT_RESOURCE
                + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_BASE;
        assertPath(resourceSubPath, securityAnnotationObj, CLASS_SECURITY_ON_BASE);
    }

    @Test
    public void test_ClassPathOnInterface_ImplOnParentResource_ImplMetWithPath() {
        var resourceSubPath = CLASS_PATH_ON_INTERFACE + PATH_SEPARATOR + CLASS_PATH_ON_INTERFACE
                + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH;
        assertNotFound(resourceSubPath);
    }

    @EnumSource(SecurityAnnotation.class)
    @ParameterizedTest
    public void test_ClassPathOnResource_ImplOnParentResource_ImplMetWithPath(Object securityAnnotationObj) {
        var resourceSubPath = CLASS_PATH_ON_RESOURCE + PATH_SEPARATOR + CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT
                + IMPL_METHOD_WITH_PATH;
        assertPath(resourceSubPath, securityAnnotationObj, CLASS_SECURITY_ON_PARENT);
    }

    @EnumSource(SecurityAnnotation.class)
    @ParameterizedTest
    public void test_ClassPathOnParentResource_ImplOnParentResource_ImplMetWithPath(Object securityAnnotationObj) {
        var resourceSubPath = CLASS_PATH_ON_PARENT_RESOURCE + PATH_SEPARATOR + CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT
                + IMPL_METHOD_WITH_PATH;
        assertPath(resourceSubPath, securityAnnotationObj, CLASS_SECURITY_ON_PARENT);
    }

    @EnumSource(SecurityAnnotation.class)
    @ParameterizedTest
    public void test_ClassPathOnInterface_ImplOnParentResource_InterfaceMetWithPath(Object securityAnnotationObj) {
        var resourceSubPath = CLASS_PATH_ON_INTERFACE + PATH_SEPARATOR + CLASS_PATH_ON_INTERFACE + IMPL_ON_PARENT
                + INTERFACE_METHOD_WITH_PATH;
        assertPath(resourceSubPath, securityAnnotationObj, CLASS_SECURITY_ON_PARENT);
    }

    @EnumSource(SecurityAnnotation.class)
    @ParameterizedTest
    public void test_ClassPathOnResource_ImplOnParentResource_InterfaceMetWithPath(Object securityAnnotationObj) {
        var resourceSubPath = CLASS_PATH_ON_RESOURCE + PATH_SEPARATOR + CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT
                + INTERFACE_METHOD_WITH_PATH;
        assertPath(resourceSubPath, securityAnnotationObj, CLASS_SECURITY_ON_PARENT);
    }

    @EnumSource(SecurityAnnotation.class)
    @ParameterizedTest
    public void test_ClassPathOnParentResource_ImplOnParentResource_InterfaceMetWithPath(Object securityAnnotationObj) {
        var resourceSubPath = CLASS_PATH_ON_PARENT_RESOURCE + PATH_SEPARATOR + CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT
                + INTERFACE_METHOD_WITH_PATH;
        assertPath(resourceSubPath, securityAnnotationObj, CLASS_SECURITY_ON_PARENT);
    }

    @EnumSource(SecurityAnnotation.class)
    @ParameterizedTest
    public void test_ClassPathOnInterface_ImplOnInterface_ImplMetWithPath(Object securityAnnotationObj) {
        var resourceSubPath = CLASS_PATH_ON_INTERFACE + PATH_SEPARATOR + CLASS_PATH_ON_INTERFACE + IMPL_ON_INTERFACE
                + IMPL_METHOD_WITH_PATH;
        assertPath(resourceSubPath, securityAnnotationObj, CLASS_SECURITY_ON_INTERFACE);
    }

    @EnumSource(SecurityAnnotation.class)
    @ParameterizedTest
    public void test_ClassPathOnResource_ImplOnInterface_ImplMetWithPath(Object securityAnnotationObj) {
        var resourceSubPath = CLASS_PATH_ON_RESOURCE + PATH_SEPARATOR + CLASS_PATH_ON_RESOURCE + IMPL_ON_INTERFACE
                + IMPL_METHOD_WITH_PATH;
        assertPath(resourceSubPath, securityAnnotationObj, CLASS_SECURITY_ON_INTERFACE);
    }

    @EnumSource(SecurityAnnotation.class)
    @ParameterizedTest
    public void test_ClassPathOnParentResource_ImplOnInterface_ImplMetWithPath(Object securityAnnotationObj) {
        var resourceSubPath = CLASS_PATH_ON_PARENT_RESOURCE + PATH_SEPARATOR + CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_INTERFACE
                + IMPL_METHOD_WITH_PATH;
        assertPath(resourceSubPath, securityAnnotationObj, CLASS_SECURITY_ON_INTERFACE);
    }

}
