package io.quarkus.security.test.permissionsallowed;

import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;

import java.util.Collections;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.StringPermission;
import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusExtensionTest;

public class PermissionsAllowedMultipleColonsTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(IdentityMock.class, AuthData.class, SecurityTestUtils.class));

    @Inject
    SecuredBean bean;

    @Test
    public void testPermissionWithActionContainingColons() {
        // @PermissionsAllowed("system:role:query1") is parsed as permission "system" and action "role:query1"
        var allowed = new AuthData(Collections.singleton("user"), false, "user",
                Set.of(new StringPermission("system", "role:query1")));
        assertSuccess(() -> bean.query(), "query", allowed);

        var wrongAction = new AuthData(Collections.singleton("user"), false, "user",
                Set.of(new StringPermission("system", "role:update")));
        assertFailureFor(() -> bean.query(), ForbiddenException.class, wrongAction);

        var wrongPermission = new AuthData(Collections.singleton("user"), false, "user",
                Set.of(new StringPermission("system:role:query1")));
        assertFailureFor(() -> bean.query(), ForbiddenException.class, wrongPermission);
    }

    @Test
    public void testMultipleActionsWithColonsAreAggregated() {
        // "system:role:query" and "system:role:update" aggregate into one StringPermission with both actions
        var withQuery = new AuthData(Collections.singleton("user"), false, "user",
                Set.of(new StringPermission("system", "role:query")));
        assertSuccess(() -> bean.queryOrUpdate(), "queryOrUpdate", withQuery);

        var withUpdate = new AuthData(Collections.singleton("user"), false, "user",
                Set.of(new StringPermission("system", "role:update")));
        assertSuccess(() -> bean.queryOrUpdate(), "queryOrUpdate", withUpdate);

        var withNeither = new AuthData(Collections.singleton("user"), false, "user",
                Set.of(new StringPermission("system", "role:delete")));
        assertFailureFor(() -> bean.queryOrUpdate(), ForbiddenException.class, withNeither);
    }

    @Singleton
    public static class SecuredBean {

        @PermissionsAllowed("system:role:query1")
        public String query() {
            return "query";
        }

        @PermissionsAllowed({ "system:role:query", "system:role:update" })
        public String queryOrUpdate() {
            return "queryOrUpdate";
        }
    }
}
