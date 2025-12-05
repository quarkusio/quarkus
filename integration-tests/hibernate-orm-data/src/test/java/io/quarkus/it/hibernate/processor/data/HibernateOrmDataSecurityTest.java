package io.quarkus.it.hibernate.processor.data;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.it.hibernate.processor.data.pudefault.MyEntity;
import io.quarkus.it.hibernate.processor.data.security.SecuredMyEntityResource;
import io.quarkus.it.hibernate.processor.data.security.SecuredMyOtherEntityResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;

@QuarkusTest
public class HibernateOrmDataSecurityTest {

    @TestSecurity(user = "hudson", roles = "admin")
    @TestHTTPEndpoint(SecuredMyEntityResource.class)
    @Test
    void testMethodLevelRolesAllowed() {
        try {
            // this Jakarta Data repository requires "root" role, but we have "admin"
            given()
                    .body(new MyEntity("foo"))
                    .contentType(ContentType.JSON)
                    .when().post("/insert-root")
                    .then()
                    .statusCode(403);
            findEntityByName("foo").statusCode(404);

            // this Jakarta Data repository requires "admin" role and we have it
            given()
                    .body(new MyEntity("foo"))
                    .contentType(ContentType.JSON)
                    .when().post("/insert-admin")
                    .then()
                    .statusCode(204);
            findEntityByName("foo").statusCode(200);
        } finally {
            deleteEntityByName("foo");
        }
    }

    @TestSecurity(user = "hudson", roles = "trump")
    @TestHTTPEndpoint(SecuredMyEntityResource.class)
    @Test
    void testMethodLevelRolesAllowedPropertyExpansion() {
        try {
            // prepare some entities
            createEntityPublic("foo");
            createEntityPublic("bar");

            // requires role 'bush', but we have 'trump'
            given()
                    .get("/list-all-george")
                    .then()
                    .statusCode(403);
            // requires role 'trump' as property expression was expanded: donald -> trump
            given()
                    .get("/list-all-donald-roles-allowed")
                    .then()
                    .statusCode(200)
                    .body("size()", is(2));
        } finally {
            deleteEntityByName("foo");
            deleteEntityByName("bar");
        }
    }

    @TestSecurity(user = "hudson", permissions = "rename-2")
    @TestHTTPEndpoint(SecuredMyEntityResource.class)
    @Test
    void testMethodLevelSinglePermissionsAllowed() {
        String entityName = "bar";
        try {
            createEntityPublic(entityName);
            findEntityByName(entityName).statusCode(200);
            findEntityByName("foo").statusCode(404);

            // this update operation requires permission 'rename-1', but we have 'rename-2'
            given()
                    .pathParam("before", entityName)
                    .pathParam("after", "foo")
                    .contentType(ContentType.JSON)
                    .post("/rename-1/{before}/to/{after}")
                    .then()
                    .statusCode(403);
            // this update operation requires permission 'rename-2' and we have it
            given()
                    .pathParam("before", entityName)
                    .pathParam("after", "foo")
                    .contentType(ContentType.JSON)
                    .post("/rename-2/{before}/to/{after}")
                    .then()
                    .statusCode(204);
            entityName = "foo";
            // check update succeeded
            findEntityByName("bar").statusCode(404);
            findEntityByName(entityName).statusCode(200);
        } finally {
            deleteEntityByName(entityName);
        }
    }

    @TestSecurity(user = "hudson", permissions = { "rename-2", "rename-3" })
    @TestHTTPEndpoint(SecuredMyEntityResource.class)
    @Test
    void testMethodLevelMultiplePermissionsAllowed() {
        String entityName1 = "foo";
        String entityName2 = "bar";
        try {
            // create 2 entities we will later try to rename
            createEntityPublic(entityName1);
            findEntityByName(entityName1).statusCode(200);
            createEntityPublic(entityName2);
            findEntityByName(entityName2).statusCode(200);

            // this repository method requires permissions 'rename-1' and 'rename-2', but we only have 'rename-2'
            given()
                    .queryParam("before", entityName1, entityName2)
                    .queryParam("after", "foobar", "baz")
                    .contentType(ContentType.JSON)
                    .post("/rename-all-perms-1-2")
                    .then()
                    .statusCode(403);
            findEntityByName("foobar").statusCode(404);
            findEntityByName("baz").statusCode(404);
            // this repository method requires permissions 'rename-2' and 'rename-3', and we have them
            given()
                    .queryParam("before", entityName1, entityName2)
                    .queryParam("after", "foobar", "baz")
                    .contentType(ContentType.JSON)
                    .post("/rename-all-perms-2-3")
                    .then()
                    .statusCode(204);
            entityName1 = "foobar";
            entityName2 = "baz";
            findEntityByName(entityName1).statusCode(200);
            findEntityByName(entityName2).statusCode(200);
        } finally {
            deleteEntityByName(entityName1);
            deleteEntityByName(entityName2);
        }
    }

    @Test
    @TestHTTPEndpoint(SecuredMyEntityResource.class)
    void testOverloadedSecuredMethod() {
        /*
         * Here we test 2 repository methods with the same name defined on the same interface, only one must be secured.
         *
         * @PermissionsAllowed("rename-overloaded")
         *
         * @Update
         * void renameOverloaded(List<MyEntity> entities);
         *
         * @Update
         * void renameOverloaded(MyEntity entity);
         */
        String entityName = "baz";
        try {
            createEntityPublic(entityName);
            findEntityByName(entityName).statusCode(200);

            given()
                    .queryParam("before", entityName)
                    .queryParam("after", "foobar")
                    .contentType(ContentType.JSON)
                    .post("/rename-overloaded-secured")
                    .then()
                    .statusCode(401);
            findEntityByName("foobar").statusCode(404);
            given()
                    .queryParam("before", entityName)
                    .queryParam("after", "foobar")
                    .contentType(ContentType.JSON)
                    .post("/rename-overloaded-public")
                    .then()
                    .statusCode(204);
            entityName = "foobar";
            findEntityByName(entityName).statusCode(200);
        } finally {
            deleteEntityByName(entityName);
        }
    }

    @TestSecurity(user = "hudson", permissions = "write-2")
    @TestHTTPEndpoint(SecuredMyEntityResource.class)
    @Test
    void testMethodLevelPermissionsAllowedMetaAnnotation() {
        String entityName = "baz";
        try {
            // @CanWrite1 requires permission 'write-1', but we have 'write-2'
            given()
                    .body(new MyEntity(entityName))
                    .contentType(ContentType.JSON)
                    .when().post("/insert-all-1")
                    .then()
                    .statusCode(403);
            findEntityByName(entityName).statusCode(404);
            // @CanWrite2 requires permission 'write-2', and we have it
            given()
                    .body(new MyEntity(entityName))
                    .contentType(ContentType.JSON)
                    .when().post("/insert-all-2")
                    .then()
                    .statusCode(204);
            findEntityByName(entityName).statusCode(200);
        } finally {
            deleteEntityByName(entityName);
        }
    }

    @TestSecurity(user = "hudson")
    @TestHTTPEndpoint(SecuredMyEntityResource.class)
    @Test
    void testMethodLevelDenyAll() {
        given()
                .body(new MyEntity("foo"))
                .contentType(ContentType.JSON)
                .when().post("/insert-deny-all")
                .then()
                .statusCode(403);
        findEntityByName("foo").statusCode(404);
    }

    @TestHTTPEndpoint(SecuredMyEntityResource.class)
    @Test
    void testMethodLevelAuthenticated() {
        given()
                .body(new MyEntity("foo"))
                .contentType(ContentType.JSON)
                .when().post("/insert-authenticated")
                .then()
                .statusCode(401);
        findEntityByName("foo").statusCode(404);
    }

    @TestSecurity(user = "hudson")
    @TestHTTPEndpoint(SecuredMyOtherEntityResource.class)
    @Test
    void testClassLevelRolesAllowed_accessDenied() {
        testClassLevelAnnotationForbidden("roles-allowed");
    }

    @TestSecurity(user = "hudson", roles = "admin")
    @TestHTTPEndpoint(SecuredMyOtherEntityResource.class)
    @Test
    void testClassLevelRolesAllowed_accessGranted() {
        testClassLevelAnnotationSuccess("roles-allowed");
    }

    @TestSecurity(user = "hudson", permissions = "wrong-permission")
    @TestHTTPEndpoint(SecuredMyOtherEntityResource.class)
    @Test
    void testClassLevelPermissionsAllowed_accessDenied() {
        testClassLevelAnnotationForbidden("permissions-allowed");
    }

    @TestSecurity(user = "hudson", permissions = "find")
    @TestHTTPEndpoint(SecuredMyOtherEntityResource.class)
    @Test
    void testClassLevelPermissionsAllowed_accessGranted() {
        testClassLevelAnnotationSuccess("permissions-allowed");
    }

    @TestSecurity(user = "hudson", permissions = "wrong-permission")
    @TestHTTPEndpoint(SecuredMyOtherEntityResource.class)
    @Test
    void testClassLevelPermissionsAllowedMetaAnnotation_accessDenied() {
        testClassLevelAnnotationForbidden("permissions-allowed-meta-annotation");
    }

    @TestSecurity(user = "hudson", permissions = "find")
    @TestHTTPEndpoint(SecuredMyOtherEntityResource.class)
    @Test
    void testClassLevelPermissionsAllowedMetaAnnotation_accessGranted() {
        testClassLevelAnnotationSuccess("permissions-allowed-meta-annotation");
    }

    @TestHTTPEndpoint(SecuredMyOtherEntityResource.class)
    @Test
    void testClassLevelAuthenticated_accessDenied() {
        testClassLevelAnnotationUnauthorized("authenticated");
    }

    @TestSecurity(user = "hudson")
    @TestHTTPEndpoint(SecuredMyOtherEntityResource.class)
    @Test
    void testClassLevelAuthenticated_accessGranted() {
        testClassLevelAnnotationSuccess("authenticated");
    }

    @TestSecurity(user = "hudson")
    @TestHTTPEndpoint(SecuredMyOtherEntityResource.class)
    @Test
    void testClassLevelDenyAll_accessDenied() {
        testClassLevelAnnotationForbidden("deny-all");
    }

    @TestSecurity(user = "hudson")
    @TestHTTPEndpoint(SecuredMyOtherEntityResource.class)
    @Test
    void testClassLevelDenyAll_permitAllOnMethodLevel() {
        testClassLevelAnnotationSuccess("deny-all-method-with-permit-all");
    }

    @TestSecurity(user = "hudson")
    @TestHTTPEndpoint(SecuredMyOtherEntityResource.class)
    @Test
    void testRepositoryParentMethodSecured_accessGranted() {
        // here interface with @Repository annotation extends another interface with security annotations
        testFindByNameWithSecurityAnnotation("parent-method-security").statusCode(404);
    }

    @TestHTTPEndpoint(SecuredMyOtherEntityResource.class)
    @Test
    void testRepositoryParentMethodSecured_accessDenied() {
        // here interface with @Repository annotation extends another interface with security annotations
        testFindByNameWithSecurityAnnotation("parent-method-security").statusCode(401);
    }

    @TestSecurity(user = "hudson")
    @TestHTTPEndpoint(SecuredMyOtherEntityResource.class)
    @Test
    void testRepositoryParentClassSecured_accessGranted() {
        // here interface with @Repository annotation extends another interface with security annotations
        testFindByNameWithSecurityAnnotation("parent-class-security").statusCode(404);
    }

    @TestHTTPEndpoint(SecuredMyOtherEntityResource.class)
    @Test
    void testRepositoryParentClassSecured_accessDenied() {
        // here interface with @Repository annotation extends another interface with security annotations
        testFindByNameWithSecurityAnnotation("parent-class-security").statusCode(401);
    }

    @TestSecurity(user = "hudson", permissions = "find-all")
    @TestHTTPEndpoint(SecuredMyEntityResource.class)
    @Test
    void testSecuredMethodWithGenericMethodParams() {
        try {
            // prepare some entities
            createEntityPublic("foo");
            createEntityPublic("bar");

            // requires role 'trump' and we don't have it
            // successful scenario (HTTP status 200) is already tested elsewhere
            given()
                    .get("/list-all-donald-roles-allowed")
                    .then()
                    .statusCode(403);

            // @PermissionsAllowed annotation requires 2 permissions and we only have 1 of them
            given()
                    .queryParam("name", "foo")
                    .get("/list-all-donald-permissions-allowed")
                    .then()
                    .statusCode(403);

            // now request the other permission and expect success
            given()
                    .header("dynamic-permission", "find-for-donald")
                    .queryParam("name", "foo")
                    .get("/list-all-donald-permissions-allowed")
                    .then()
                    .statusCode(200)
                    .body("size()", is(1));

            given()
                    .queryParam("name", "foo")
                    .get("/list-all-donald-public")
                    .then()
                    .statusCode(200)
                    .body("size()", is(1));
        } finally {
            deleteEntityByName("foo");
            deleteEntityByName("bar");
        }
    }

    private static void testClassLevelAnnotationForbidden(String annotation) {
        testFindByNameWithSecurityAnnotation(annotation).statusCode(403);
    }

    private static void testClassLevelAnnotationUnauthorized(String annotation) {
        testFindByNameWithSecurityAnnotation(annotation).statusCode(401);
    }

    private static void testClassLevelAnnotationSuccess(String annotation) {
        // entity is unknown, therefore we expect 404
        testFindByNameWithSecurityAnnotation(annotation).statusCode(404);
    }

    private static ValidatableResponse testFindByNameWithSecurityAnnotation(String securityAnnotation) {
        return given()
                .pathParam("name", "unknown")
                .contentType(ContentType.JSON)
                .when().get("/by/name/{name}/" + securityAnnotation)
                .then();
    }

    private static void deleteEntityByName(String entityName) {
        // this doesn't check response status because if some assertion failed
        // could be legal that the entity doesn't exist, yet we want to clean-up
        // so that each test method is more likely to be isolated
        given()
                .pathParam("name", entityName)
                .contentType(ContentType.JSON)
                .when().delete("/by/name/{name}");
    }

    private static ValidatableResponse findEntityByName(String entityName) {
        return given()
                .pathParam("name", entityName)
                .contentType(ContentType.JSON)
                .when().get("/by/name/{name}")
                .then();
    }

    private static void createEntityPublic(String entityName) {
        given()
                .body(new MyEntity(entityName))
                .contentType(ContentType.JSON)
                .when().post("/insert-public")
                .then()
                .statusCode(204);
    }
}
