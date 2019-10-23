package io.quarkus.resteasy.test.security;

import static io.restassured.RestAssured.when;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.test.security.app.PermitAllResource;
import io.quarkus.resteasy.test.security.app.UnsecuredResource;
import io.quarkus.resteasy.test.security.app.UnsecuredSubResource;
import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class DenyAllJaxRsTest {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(PermitAllResource.class, UnsecuredResource.class,
                            UnsecuredSubResource.class, IdentityMock.class, AuthData.class)
                    .addAsResource("application-deny-jaxrs.properties",
                            "application.properties"));

    @Test
    public void shouldDenyUnannotated() {
        String path = "/unsecured/defaultSecurity";
        assertStatus(path, 403, IdentityMock.ANONYMOUS);
        assertStatus(path, 403, IdentityMock.USER, IdentityMock.ADMIN);
    }

    @Test
    public void shouldDenyDenyAllMethod() {
        String path = "/unsecured/denyAll";
        assertStatus(path, 403, IdentityMock.ANONYMOUS);
        assertStatus(path, 403, IdentityMock.USER, IdentityMock.ADMIN);
    }

    @Test
    public void shouldPermitPermitAllMethod() {
        assertStatus("/unsecured/permitAll", 200, IdentityMock.ANONYMOUS, IdentityMock.USER, IdentityMock.ADMIN);
    }

    @Test
    public void shouldDenySubResource() {
        String path = "/unsecured/sub/subMethod";
        assertStatus(path, 403, IdentityMock.ANONYMOUS);
        assertStatus(path, 403, IdentityMock.USER, IdentityMock.ADMIN);
    }

    @Test
    public void shouldAllowPermitAllSubResource() {
        String path = "/unsecured/permitAllSub/subMethod";
        assertStatus(path, 200, IdentityMock.ANONYMOUS, IdentityMock.USER, IdentityMock.ADMIN);
    }

    @Test
    public void shouldAllowPermitAllClass() {
        String path = "/permitAll/sub/subMethod";
        assertStatus(path, 200, IdentityMock.ANONYMOUS, IdentityMock.USER, IdentityMock.ADMIN);
    }

    private void assertStatus(String path, int status, AuthData... auths) {
        for (AuthData auth : auths) {
            IdentityMock.setUpAuth(auth);
            when().get(path)
                    .then()
                    .statusCode(status);
        }
    }

}
