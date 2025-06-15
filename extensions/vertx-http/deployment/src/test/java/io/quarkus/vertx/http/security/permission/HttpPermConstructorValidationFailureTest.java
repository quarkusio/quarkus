package io.quarkus.vertx.http.security.permission;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class HttpPermConstructorValidationFailureTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
            .addClasses(PermissionImpl.class)
            .addAsResource(new StringAsset("quarkus.http.auth.policy.t1.roles-allowed=admin\n"
                    + "quarkus.http.auth.policy.t1.permissions.test=perm1\n"
                    + "quarkus.http.auth.policy.t1.permission-class=io.quarkus.vertx.http.security.permission.PermissionImpl\n"
                    + "quarkus.http.auth.permission.t1.paths=/*\n" + "quarkus.http.auth.permission.t1.policy=t1"),
                    "application.properties"))
            .setExpectedException(ConfigurationException.class);

    @Test
    public void test() {
        Assertions.fail();
    }
}
