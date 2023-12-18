package io.quarkus.vertx.http.security;

import static org.hamcrest.Matchers.is;

import java.io.File;
import java.net.URL;
import java.security.Permission;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.StringPermission;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class MtlsRequestBasicAuthTest {

    @TestHTTPResource(value = "/mtls", ssl = true)
    URL url;

    @TestHTTPResource(value = "/mtls-augmentor", ssl = true)
    URL augmentorUrl;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class)
                    .addClasses(TestIdentityProvider.class, TestTrustedIdentityProvider.class, TestIdentityController.class)
                    .addAsResource("conf/mtls/mtls-basic-jks.conf", "application.properties")
                    .addAsResource("conf/mtls/server-keystore.jks", "server-keystore.jks")
                    .addAsResource("conf/mtls/server-truststore.jks", "server-truststore.jks"));

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin");
    }

    @Test
    public void testClientAuthentication() {
        RestAssured.given()
                .keyStore(new File("src/test/resources/conf/mtls/client-keystore.jks"), "password")
                .trustStore(new File("src/test/resources/conf/mtls/client-truststore.jks"), "password")
                .get(url).then().statusCode(200).body(is("CN=client,OU=cert,O=quarkus,L=city,ST=state,C=AU"));
    }

    @Test
    public void testNoClientCert() {
        RestAssured.given()
                .trustStore(new File("src/test/resources/conf/mtls/client-truststore.jks"), "password")
                .get(url).then().statusCode(200).body(is(""));
    }

    @Test
    public void testNoClientCertBasicAuth() {
        RestAssured.given()
                .auth()
                .preemptive()
                .basic("admin", "admin")
                .trustStore(new File("src/test/resources/conf/mtls/client-truststore.jks"), "password")
                .get(url).then().statusCode(200).body(is("admin"));
    }

    @Test
    public void testSecurityIdentityAugmentor() {
        RestAssured.given()
                .keyStore(new File("src/test/resources/conf/mtls/client-keystore.jks"), "password")
                .trustStore(new File("src/test/resources/conf/mtls/client-truststore.jks"), "password")
                .get(augmentorUrl).then().statusCode(401);
        RestAssured.given()
                .header("add-perm", "true")
                .keyStore(new File("src/test/resources/conf/mtls/client-keystore.jks"), "password")
                .trustStore(new File("src/test/resources/conf/mtls/client-truststore.jks"), "password")
                .get(augmentorUrl).then().statusCode(200);
    }

    @ApplicationScoped
    static class MyBean {

        public void register(@Observes Router router) {
            router.get("/mtls").handler(rc -> {
                rc.response().end(QuarkusHttpUser.class.cast(rc.user()).getSecurityIdentity().getPrincipal().getName());
            });
            router.get("/mtls-augmentor").handler(rc -> {
                if (rc.user() instanceof QuarkusHttpUser quarkusHttpUser) {
                    quarkusHttpUser.getSecurityIdentity().checkPermission(new StringPermission("use-mTLS"))
                            .subscribe().with(new Consumer<Boolean>() {
                                @Override
                                public void accept(Boolean accessGranted) {
                                    if (accessGranted) {
                                        rc.end();
                                    } else {
                                        rc.fail(401);
                                    }
                                }
                            });
                } else {
                    rc.fail(500);
                }
            });
        }

    }

    @ApplicationScoped
    static class CustomSecurityIdentityAugmentor implements SecurityIdentityAugmentor {

        @Override
        public Uni<SecurityIdentity> augment(SecurityIdentity securityIdentity,
                AuthenticationRequestContext authenticationRequestContext) {
            if (!securityIdentity.isAnonymous()
                    && "CN=client,OU=cert,O=quarkus,L=city,ST=state,C=AU".equals(securityIdentity.getPrincipal().getName())) {
                return Uni.createFrom().item(QuarkusSecurityIdentity.builder(securityIdentity)
                        .addPermissionChecker(new Function<Permission, Uni<Boolean>>() {
                            @Override
                            public Uni<Boolean> apply(Permission required) {
                                RoutingContext event = HttpSecurityUtils.getRoutingContextAttribute();
                                final boolean pass;
                                if (event != null) {
                                    pass = Boolean.parseBoolean(event.request().headers().get("add-perm"));
                                } else {
                                    pass = false;
                                }
                                return Uni.createFrom().item(pass);
                            }
                        }).build());
            }
            return Uni.createFrom().item(securityIdentity);
        }
    }
}
