package io.quarkus.vertx.http.security;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

public class DuplicatedNamedAuthPolicyValidationFailureTest {

    private static final String POLICY_NAME = "p_o_l_i_c_y_n_a_m_e";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(NamedPolicy_1.class, NamedPolicy_2.class))
            .assertException(throwable -> {
                var errMsg = throwable.getMessage();
                Assertions.assertTrue(errMsg.contains("Only one HttpSecurityPolicy"), errMsg);
                Assertions.assertTrue(errMsg.contains(POLICY_NAME), errMsg);
                Assertions.assertTrue(errMsg.contains(NamedPolicy_1.class.getSimpleName()));
                Assertions.assertTrue(errMsg.contains(NamedPolicy_2.class.getSimpleName()));
            });

    @Test
    public void test() {
        Assertions.fail("Build was supposed to fail due to validation");
    }

    @ApplicationScoped
    public static class NamedPolicy_1 implements HttpSecurityPolicy {

        @Override
        public Uni<CheckResult> checkPermission(RoutingContext request, Uni<SecurityIdentity> identity,
                AuthorizationRequestContext requestContext) {
            return null;
        }

        @Override
        public String name() {
            return POLICY_NAME;
        }
    }

    @ApplicationScoped
    public static class NamedPolicy_2 implements HttpSecurityPolicy {

        @Override
        public Uni<CheckResult> checkPermission(RoutingContext request, Uni<SecurityIdentity> identity,
                AuthorizationRequestContext requestContext) {
            return null;
        }

        @Override
        public String name() {
            return POLICY_NAME;
        }
    }
}
