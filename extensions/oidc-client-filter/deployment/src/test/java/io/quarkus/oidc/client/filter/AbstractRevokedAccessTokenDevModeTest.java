package io.quarkus.oidc.client.filter;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;

import jakarta.inject.Inject;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;

import io.quarkus.oidc.client.NamedOidcClient;
import io.quarkus.oidc.client.OidcClient;
import io.quarkus.security.Authenticated;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public abstract class AbstractRevokedAccessTokenDevModeTest {

    protected static final String NAMED_CLIENT = "named";
    protected static final String RESPONSE = "Ho hey!";
    protected static final String MY_CLIENT_RESOURCE_PATH = "/my-client-resource";
    protected static final String MY_SERVER_RESOURCE_PATH = "/my-server-resource";
    private static final Class<?>[] BASE_TEST_CLASSES = {
            MyServerResource.class, MyClientResource.class, MyClient.class, MyClientCategory.class,
            AbstractRevokedAccessTokenDevModeTest.class
    };

    protected static QuarkusDevModeTest createQuarkusDevModeTest(String additionalProperties,
            Class<? extends MyClient> defaultClientClass,
            Class<? extends MyClient> namedClientClass, Class<? extends MyClient> namedClientWithoutRefresh,
            Class<? extends MyClient> defaultClientWithoutRefresh, Class<? extends MyClientResource> myClientResourceImpl,
            Class<?>... additionalClasses) {
        return new QuarkusDevModeTest().withApplicationRoot((jar) -> jar
                .addAsResource(
                        new StringAsset("""
                                %s/mp-rest/url=http://localhost:${quarkus.http.port}
                                %s/mp-rest/url=http://localhost:${quarkus.http.port}
                                %s/mp-rest/url=http://localhost:${quarkus.http.port}
                                %s/mp-rest/url=http://localhost:${quarkus.http.port}
                                quarkus.oidc.auth-server-url=${keycloak.url}/realms/quarkus/
                                quarkus.oidc.client-id=quarkus-service-app
                                quarkus.oidc.credentials.secret=secret
                                quarkus.oidc.token.verify-access-token-with-user-info=true
                                quarkus.oidc-client.auth-server-url=${quarkus.oidc.auth-server-url}
                                quarkus.oidc-client.client-id=${quarkus.oidc.client-id}
                                quarkus.oidc-client.credentials.client-secret.value=${quarkus.oidc.credentials.secret}
                                quarkus.oidc-client.credentials.client-secret.method=POST
                                quarkus.oidc-client.grant.type=password
                                quarkus.oidc-client.grant-options.password.username=alice
                                quarkus.oidc-client.grant-options.password.password=alice
                                quarkus.oidc-client.scopes=openid
                                quarkus.oidc-client.named.scopes=openid
                                quarkus.oidc-client.named.auth-server-url=${quarkus.oidc.auth-server-url}
                                quarkus.oidc-client.named.client-id=${quarkus.oidc.client-id}
                                quarkus.oidc-client.named.credentials.client-secret.value=${quarkus.oidc.credentials.secret}
                                quarkus.oidc-client.named.credentials.client-secret.method=POST
                                quarkus.oidc-client.named.grant.type=password
                                quarkus.oidc-client.named.grant-options.password.username=alice
                                quarkus.oidc-client.named.grant-options.password.password=alice
                                %s
                                """.formatted(defaultClientClass.getName(), namedClientClass.getName(),
                                namedClientWithoutRefresh.getName(), defaultClientWithoutRefresh.getName(),
                                additionalProperties)),
                        "application.properties")
                .addClasses(defaultClientClass, namedClientClass, namedClientWithoutRefresh, defaultClientWithoutRefresh,
                        myClientResourceImpl)
                .addClasses(additionalClasses)
                .addClasses(BASE_TEST_CLASSES));
    }

    void verifyTokenRefreshedOn401(MyClientCategory myClientCategory) {
        RestAssured.given()
                .body(myClientCategory)
                .post(MY_CLIENT_RESOURCE_PATH)
                .then().statusCode(200)
                .body(Matchers.is(RESPONSE));
        // access token is revoked now
        RestAssured.given()
                .body(myClientCategory)
                .post(MY_CLIENT_RESOURCE_PATH)
                .then().statusCode(401);
        // response filter recognized 401 and told the request token to refresh the token on next request
        RestAssured.given()
                .body(myClientCategory)
                .post(MY_CLIENT_RESOURCE_PATH)
                .then().statusCode(200)
                .body(Matchers.is(RESPONSE));
    }

    @Test
    void verifyDefaultClientHasNotRefreshedTokenOnUnauthorized() {
        RestAssured.given()
                .body(MyClientCategory.DEFAULT_CLIENT_WITHOUT_REFRESH)
                .post(MY_CLIENT_RESOURCE_PATH)
                .then().statusCode(200)
                .body(Matchers.is(RESPONSE));
        // access token is revoked now
        RestAssured.given()
                .body(MyClientCategory.DEFAULT_CLIENT_WITHOUT_REFRESH)
                .post(MY_CLIENT_RESOURCE_PATH)
                .then().statusCode(401);
        // there is no response filter, so our request filter still doesn't know that the token is revoked
        RestAssured.given()
                .body(MyClientCategory.DEFAULT_CLIENT_WITHOUT_REFRESH)
                .post(MY_CLIENT_RESOURCE_PATH)
                .then().statusCode(401);
    }

    @Test
    void verifyNamedClientHasNotRefreshedTokenOnUnauthorized() {
        RestAssured.given()
                .body(MyClientCategory.NAMED_CLIENT_WITHOUT_REFRESH)
                .post(MY_CLIENT_RESOURCE_PATH)
                .then().statusCode(200)
                .body(Matchers.is(RESPONSE));
        // access token is revoked now
        RestAssured.given()
                .body(MyClientCategory.NAMED_CLIENT_WITHOUT_REFRESH)
                .post(MY_CLIENT_RESOURCE_PATH)
                .then().statusCode(401);
        // there is no response filter, so our request filter still doesn't know that the token is revoked
        RestAssured.given()
                .body(MyClientCategory.NAMED_CLIENT_WITHOUT_REFRESH)
                .post(MY_CLIENT_RESOURCE_PATH)
                .then().statusCode(401);
    }

    @Path(MY_SERVER_RESOURCE_PATH)
    public static class MyServerResource {

        private volatile String previousToken = null;

        @Inject
        OidcClient defaultOidcClient;

        @NamedOidcClient(NAMED_CLIENT)
        @Inject
        OidcClient namedOidcClient;

        @Authenticated
        @POST
        public String revokeAccessTokenAndRespond(@HeaderParam(AUTHORIZATION) String authorizationHeader, String named) {
            String accessToken = authorizationHeader.substring("Bearer ".length());
            if (accessToken.equals(previousToken)) {
                // do not expect this to happen
                throw new IllegalStateException("Quarkus OIDC should recognize this access token is revoked");
            }
            previousToken = accessToken;
            OidcClient client = Boolean.parseBoolean(named) ? namedOidcClient : defaultOidcClient;
            boolean tokenRevoked = client.revokeAccessToken(accessToken).await().indefinitely();
            if (tokenRevoked) {
                return RESPONSE;
            }
            // do not expect this to happen
            throw new IllegalStateException("Token is not revoked");
        }

    }

    @Path(MY_CLIENT_RESOURCE_PATH)
    public abstract static class MyClientResource {

        @POST
        public String talkToServerAndRespond(MyClientCategory clientCategory) {
            String named = clientCategory.named + "";
            return switch (clientCategory) {
                case NAMED_CLIENT -> myNamedClient().revokeAccessTokenAndRespond(named);
                case DEFAULT_CLIENT -> myDefaultClient().revokeAccessTokenAndRespond(named);
                case DEFAULT_CLIENT_WITHOUT_REFRESH -> myDefaultClientWithoutRefresh().revokeAccessTokenAndRespond(named);
                case NAMED_CLIENT_WITHOUT_REFRESH -> myNamedClientWithoutRefresh().revokeAccessTokenAndRespond(named);
                // calls the same endpoint as ones above, however with filter applied on individual RESTEasy client methods
                case NAMED_CLIENT_ANNOTATION_ON_METHOD -> myNamedClient_AnnotationOnMethod(named);
                case DEFAULT_CLIENT_ANNOTATION_ON_METHOD -> myDefaultClient_AnnotationOnMethod(named);
                case NAMED_CLIENT_MULTIPLE_METHODS -> myNamedClient_MultipleMethods(named);
                case DEFAULT_CLIENT_MULTIPLE_METHODS -> myDefaultClient_MultipleMethods(named);
                case NO_ACCESS_TOKEN -> multipleMethods_noAccessToken();
            };
        }

        protected abstract MyClient myDefaultClient();

        protected abstract MyClient myNamedClient();

        protected abstract MyClient myDefaultClientWithoutRefresh();

        protected abstract MyClient myNamedClientWithoutRefresh();

        protected String myDefaultClient_AnnotationOnMethod(String named) {
            return null;
        }

        protected String myNamedClient_AnnotationOnMethod(String named) {
            return null;
        }

        protected String myDefaultClient_MultipleMethods(String named) {
            return null;
        }

        protected String myNamedClient_MultipleMethods(String named) {
            return null;
        }

        protected String multipleMethods_noAccessToken() {
            return null;
        }

    }

    public interface MyClient {

        @POST
        String revokeAccessTokenAndRespond(String named);

    }

    public enum MyClientCategory {
        DEFAULT_CLIENT(false),
        NAMED_CLIENT(true),
        DEFAULT_CLIENT_WITHOUT_REFRESH(false),
        NAMED_CLIENT_WITHOUT_REFRESH(true),
        DEFAULT_CLIENT_ANNOTATION_ON_METHOD(false),
        NAMED_CLIENT_ANNOTATION_ON_METHOD(true),
        NAMED_CLIENT_MULTIPLE_METHODS(true),
        DEFAULT_CLIENT_MULTIPLE_METHODS(false),
        NO_ACCESS_TOKEN(false);

        public final boolean named;

        MyClientCategory(boolean named) {
            this.named = named;
        }
    }
}
