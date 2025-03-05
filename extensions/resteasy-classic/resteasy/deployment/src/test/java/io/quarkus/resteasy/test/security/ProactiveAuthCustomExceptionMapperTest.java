package io.quarkus.resteasy.test.security;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ProactiveAuthCustomExceptionMapperTest extends AbstractCustomExceptionMapperTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest().withApplicationRoot(jar -> jar
            .addAsResource(new StringAsset("""
                    quarkus.http.auth.permission.authentication.paths=*
                    quarkus.http.auth.permission.authentication.policy=authenticated
                    """), "application.properties"));

}
