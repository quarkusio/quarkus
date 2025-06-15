package io.quarkus.vertx.http.security.permission;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.security.CustomPermission;
import io.quarkus.vertx.http.security.CustomPermissionWithActions;

public class HttpSecPolicyGrantingPermissionsLazyAuthTest extends AbstractHttpSecurityPolicyGrantingPermissionsTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
            .addClasses(TestIdentityController.class, TestIdentityProvider.class, PermissionsPathHandler.class,
                    CDIBean.class, CustomPermission.class, CustomPermissionWithActions.class)
            .addAsResource("conf/http-permission-grant-config.properties", "application.properties").addAsResource(
                    new StringAsset("quarkus.http.auth.proactive=false\n"), "META-INF/microprofile-config.properties"));

}
