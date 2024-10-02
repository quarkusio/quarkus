package io.quarkus.resteasy.reactive.server.test.security;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;

public class LazyAuthPermissionsAllowedTestCase extends AbstractPermissionsAllowedTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(PermissionsAllowedResource.class, TestIdentityProvider.class, TestIdentityController.class,
                            NonBlockingPermissionsAllowedResource.class, CustomPermission.class,
                            PermissionsIdentityAugmentor.class, CustomPermissionWithExtraArgs.class,
                            StringPermissionsAllowedMetaAnnotation.class, CreateOrUpdate.class)
                    .addAsResource(new StringAsset("quarkus.http.auth.proactive=false\n"),
                            "application.properties"));

}
