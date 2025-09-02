package io.quarkus.oidc.test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.AuthenticationContext;
import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.test.QuarkusUnitTest;

public class ProactiveAuthenticationContextValidationFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    // starting the dev service would be a waste
                    .addClass(StepUpAuthResource.class)
                    .addAsResource(new StringAsset("quarkus.devservices.enabled=false"), "application.properties"))
            .assertException(t -> {
                Throwable rootCause = ExceptionUtil.getRootCause(t);
                Assertions.assertNotNull(rootCause);
                String message = rootCause.getMessage();
                Assertions.assertNotNull(message);
                Assertions.assertTrue(message.contains("proactive authentication is disabled"), message);
            });

    @Test
    public void test() {
        Assertions.fail("Validation should fail");
    }

    @AuthenticationContext("ignored")
    @Path("step-up-auth")
    public static class StepUpAuthResource {

        @GET
        public String stepUpAuth() {
            return "step-up-auth";
        }

    }
}
