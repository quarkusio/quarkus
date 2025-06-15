package io.quarkus.security.test.permissionsallowed;

import java.security.Permission;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.StringPermission;
import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class MethodLevelStringPermissionsAllowedTest extends AbstractMethodLevelPermissionsAllowedTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(IdentityMock.class, AuthData.class, SecurityTestUtils.class));

    @Inject
    PermissionsAllowedNameOnlyBean nameOnlyBean;

    @Inject
    PermissionsAllowedNameAndActionsOnlyBean nameAndActionsBean;

    @Inject
    MultiplePermissionsAllowedBean multiplePermissionsAllowedBean;

    @Override
    protected MultiplePermissionsAllowedBeanI getMultiplePermissionsAllowedBean() {
        return multiplePermissionsAllowedBean;
    }

    @Override
    protected PermissionsAllowedNameOnlyBeanI getPermissionsAllowedNameOnlyBean() {
        return nameOnlyBean;
    }

    @Override
    protected PermissionsAllowedNameAndActionsOnlyBeanI getPermissionsAllowedNameAndActionsOnlyBean() {
        return nameAndActionsBean;
    }

    @Override
    protected Permission createPermission(String name, String... actions) {
        return new StringPermission(name, actions);
    }

    @Singleton
    public static class PermissionsAllowedNameOnlyBean implements PermissionsAllowedNameOnlyBeanI {

        @PermissionsAllowed(WRITE_PERMISSION)
        public final String write() {
            return WRITE_PERMISSION;
        }

        @PermissionsAllowed(READ_PERMISSION)
        public final String read() {
            return READ_PERMISSION;
        }

        @PermissionsAllowed(WRITE_PERMISSION)
        public final Uni<String> writeNonBlocking() {
            return Uni.createFrom().item(WRITE_PERMISSION);
        }

        @PermissionsAllowed(READ_PERMISSION)
        public final Uni<String> readNonBlocking() {
            return Uni.createFrom().item(READ_PERMISSION);
        }

        @PermissionsAllowed("prohibited")
        public final void prohibited() {
        }

        @PermissionsAllowed("prohibited")
        public final Uni<Void> prohibitedNonBlocking() {
            return Uni.createFrom().nullItem();
        }

        @PermissionsAllowed({ "one", "two", "three", READ_PERMISSION })
        public final String multiple() {
            return MULTIPLE_PERMISSION;
        }

        @PermissionsAllowed({ "one", "two", "three", READ_PERMISSION })
        public final Uni<String> multipleNonBlocking() {
            return Uni.createFrom().item(MULTIPLE_PERMISSION);
        }

    }

    @Singleton
    public static class PermissionsAllowedNameAndActionsOnlyBean implements PermissionsAllowedNameAndActionsOnlyBeanI {

        @PermissionsAllowed(WRITE_PERMISSION_BEAN)
        public final String write() {
            return WRITE_PERMISSION_BEAN;
        }

        @PermissionsAllowed(READ_PERMISSION_BEAN)
        public final String read() {
            return READ_PERMISSION_BEAN;
        }

        @PermissionsAllowed(WRITE_PERMISSION_BEAN)
        public final Uni<String> writeNonBlocking() {
            return Uni.createFrom().item(WRITE_PERMISSION_BEAN);
        }

        @PermissionsAllowed(READ_PERMISSION_BEAN)
        public final Uni<String> readNonBlocking() {
            return Uni.createFrom().item(READ_PERMISSION_BEAN);
        }

        @PermissionsAllowed("prohibited:bean")
        public final void prohibited() {
        }

        @PermissionsAllowed("prohibited:bean")
        public final Uni<Void> prohibitedNonBlocking() {
            return Uni.createFrom().nullItem();
        }

        @PermissionsAllowed({ "one:a", "two:b", "three:c", READ_PERMISSION_BEAN })
        public final String multiple() {
            return MULTIPLE_BEAN;
        }

        @PermissionsAllowed({ "one:a", "two:b", "three:c", READ_PERMISSION_BEAN })
        public final Uni<String> multipleNonBlocking() {
            return Uni.createFrom().item(MULTIPLE_BEAN);
        }

        @PermissionsAllowed({ "one:a", "two:b", "three:c", "one:b", "two:a", "three:a", READ_PERMISSION_BEAN,
                "read:meal" })
        public final String multipleActions() {
            return MULTIPLE_BEAN;
        }

        @PermissionsAllowed({ "one:a", "two:b", "three:c", "one:b", "two:a", "three:a", READ_PERMISSION_BEAN,
                "read:meal" })
        public final Uni<String> multipleNonBlockingActions() {
            return Uni.createFrom().item(MULTIPLE_BEAN);
        }

        @PermissionsAllowed({ "one", "two", "three:c", "three", READ_PERMISSION })
        public final String combination() {
            return MULTIPLE_BEAN;
        }

        @PermissionsAllowed({ "one", "two", "three:c", "three", READ_PERMISSION })
        public final Uni<String> combinationNonBlockingActions() {
            return Uni.createFrom().item(MULTIPLE_BEAN);
        }

        @PermissionsAllowed({ "one", "two", "three:c", "three", "read:bread", "read:meal" })
        public final String combination2() {
            return "combination2";
        }

        @PermissionsAllowed({ "one", "two", "three:c", "three", "read:bread", "read:meal" })
        public final Uni<String> combination2NonBlockingActions() {
            return Uni.createFrom().item("combination2");
        }

        @PermissionsAllowed(value = { "one", "two", "three:c", "three", "read:bread", "read:meal" }, inclusive = true)
        @Override
        public User inclusive() {
            return new User("Martin");
        }

        @PermissionsAllowed(value = { "one", "two", "three:c", "three", "read:bread", "read:meal" }, inclusive = true)
        @Override
        public Uni<User> inclusiveNonBlocking() {
            return Uni.createFrom().item(new User("Bruno"));
        }

    }

    @Singleton
    public static class MultiplePermissionsAllowedBean implements MultiplePermissionsAllowedBeanI {

        @PermissionsAllowed("update")
        @PermissionsAllowed("create")
        public String createOrUpdate() {
            return "create_or_update";
        }

        @PermissionsAllowed("create")
        @PermissionsAllowed("update")
        public Uni<String> createOrUpdateNonBlocking() {
            return Uni.createFrom().item("create_or_update");
        }

        @PermissionsAllowed("see")
        @PermissionsAllowed("view")
        public String getOne() {
            return "see_or_view_detail";
        }

        @PermissionsAllowed("view")
        @PermissionsAllowed("see")
        public Uni<String> getOneNonBlocking() {
            return Uni.createFrom().item("see_or_view_detail");
        }

        @PermissionsAllowed({ "operand1", "operand2", "operand3" })
        @PermissionsAllowed({ "operand4", "operand5" })
        @PermissionsAllowed({ "operand6", "operand7" })
        @PermissionsAllowed({ "operand8" })
        public Uni<String> predicateNonBlocking() {
            // (operand1 OR operand2 OR operand3) AND (operand4 OR operand5) AND (operand6 OR operand7) AND operand8
            return Uni.createFrom().item(PREDICATE);
        }

        @PermissionsAllowed({ "operand1", "operand2", "operand3" })
        @PermissionsAllowed({ "operand4", "operand5" })
        @PermissionsAllowed({ "operand6", "operand7" })
        @PermissionsAllowed({ "operand8" })
        public String predicate() {
            // (permission1:action1 OR permission2:action2) AND (permission1:action2 OR permission2:action1)
            return PREDICATE;
        }

        @PermissionsAllowed({ "permission1:action1", "permission2:action2" })
        @PermissionsAllowed({ "permission1:action2", "permission2:action1" })
        public String actionsPredicate() {
            // (permission1:action1 OR permission2:action2) AND (permission1:action2 OR permission2:action1)
            return PREDICATE;
        }

        @PermissionsAllowed({ "permission1:action1", "permission2:action2" })
        @PermissionsAllowed({ "permission1:action2", "permission2:action1" })
        public Uni<String> actionsPredicateNonBlocking() {
            // (permission1:action1 OR permission2:action2) AND (permission1:action2 OR permission2:action1)
            return Uni.createFrom().item(PREDICATE);
        }

        @PermissionsAllowed(value = { "permission1:action1", "permission2:action2" }, inclusive = true)
        @PermissionsAllowed(value = { "permission1:action2", "permission2:action1" }, inclusive = true)
        @Override
        public User inclusive() {
            return new User("Sergey");
        }

        @PermissionsAllowed(value = { "permission1:action1", "permission2:action2" }, inclusive = true)
        @PermissionsAllowed(value = { "permission1:action2", "permission2:action1" }, inclusive = true)
        @Override
        public Uni<User> inclusiveNonBlocking() {
            return Uni.createFrom().item(new User("Stuart"));
        }

    }

}
