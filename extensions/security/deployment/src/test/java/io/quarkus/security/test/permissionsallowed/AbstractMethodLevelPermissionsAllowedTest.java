package io.quarkus.security.test.permissionsallowed;

import static io.quarkus.security.test.utils.IdentityMock.ANONYMOUS;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;

import java.security.Permission;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.test.utils.AuthData;
import io.smallrye.mutiny.Uni;

public abstract class AbstractMethodLevelPermissionsAllowedTest {
    protected static final String READ_PERMISSION = "read";
    protected static final String WRITE_PERMISSION = "write";
    protected static final String MULTIPLE_PERMISSION = "multiple";
    protected static final String READ_PERMISSION_BEAN = "read:bean";
    protected static final String WRITE_PERMISSION_BEAN = "write:bean";
    protected static final String MULTIPLE_BEAN = "multiple:bean";
    protected static final String PREDICATE = "predicate";
    protected final AuthData USER = new AuthData(Collections.singleton("user"), false, "user",
            Set.of(createPermission("read", (String[]) null)));
    protected final AuthData ADMIN = new AuthData(Collections.singleton("admin"), false, "admin",
            Set.of(createPermission("write", (String[]) null)));

    @Test
    public void shouldRestrictAccessToSpecificPermissionName() {
        // identity has one permission, annotation has same permission
        assertSuccess(() -> getPermissionsAllowedNameOnlyBean().write(), WRITE_PERMISSION, ADMIN);
        assertSuccess(getPermissionsAllowedNameOnlyBean().writeNonBlocking(), WRITE_PERMISSION, ADMIN);
        assertSuccess(() -> getPermissionsAllowedNameOnlyBean().read(), READ_PERMISSION, USER);
        assertSuccess(getPermissionsAllowedNameOnlyBean().readNonBlocking(), READ_PERMISSION, USER);

        // identity has one permission, annotation has different permission
        assertFailureFor(() -> getPermissionsAllowedNameOnlyBean().prohibited(), ForbiddenException.class, ADMIN);
        assertFailureFor(getPermissionsAllowedNameOnlyBean().prohibitedNonBlocking(), ForbiddenException.class, ADMIN);

        // identity has no permission, annotation has one permission
        assertFailureFor(() -> getPermissionsAllowedNameOnlyBean().prohibited(), UnauthorizedException.class,
                ANONYMOUS);
        assertFailureFor(getPermissionsAllowedNameOnlyBean().prohibitedNonBlocking(), UnauthorizedException.class,
                ANONYMOUS);

        // identity has one permission, annotation has multiple permissions
        assertSuccess(() -> getPermissionsAllowedNameOnlyBean().multiple(), MULTIPLE_PERMISSION, USER);
        assertSuccess(getPermissionsAllowedNameOnlyBean().multipleNonBlocking(), MULTIPLE_PERMISSION, USER);

        // identity has one permission, annotation has different permissions
        assertFailureFor(() -> getPermissionsAllowedNameOnlyBean().multiple(), ForbiddenException.class, ADMIN);
        assertFailureFor(getPermissionsAllowedNameOnlyBean().multipleNonBlocking(), ForbiddenException.class, ADMIN);

        // identity has multiple permissions, annotation has multiple permissions
        final var multiplePermissionsIdentity = new AuthData(Set.of(), false, MULTIPLE_PERMISSION,
                permissions(READ_PERMISSION, "a", "b", "c", "d"));
        assertSuccess(() -> getPermissionsAllowedNameOnlyBean().multiple(), MULTIPLE_PERMISSION,
                multiplePermissionsIdentity);
        assertSuccess(getPermissionsAllowedNameOnlyBean().multipleNonBlocking(), MULTIPLE_PERMISSION,
                multiplePermissionsIdentity);

        // identity has multiple permissions, annotation has one permission
        assertSuccess(() -> getPermissionsAllowedNameOnlyBean().read(), READ_PERMISSION, multiplePermissionsIdentity);
        assertSuccess(getPermissionsAllowedNameOnlyBean().readNonBlocking(), READ_PERMISSION,
                multiplePermissionsIdentity);

        // identity has multiple permissions, annotation has different permissions
        final var diffPermissionsIdentity = new AuthData(Set.of(), false, MULTIPLE_PERMISSION,
                permissions(WRITE_PERMISSION, "a", "b", "c", "d"));
        assertFailureFor(() -> getPermissionsAllowedNameOnlyBean().multiple(), ForbiddenException.class,
                diffPermissionsIdentity);
        assertFailureFor(getPermissionsAllowedNameOnlyBean().multipleNonBlocking(), ForbiddenException.class,
                diffPermissionsIdentity);
    }

    @Test
    public void shouldRestrictAccessToSpecificPermissionAndAction() {
        final var admin = new AuthData(Collections.singleton("admin"), false, "admin",
                permission(WRITE_PERMISSION, "bean"));
        final var user = new AuthData(Collections.singleton("user"), false, "user",
                permission(READ_PERMISSION, "bean"));

        // identity has one permission and action, annotation has same permission and action
        assertSuccess(() -> getPermissionsAllowedNameAndActionsOnlyBean().write(), WRITE_PERMISSION_BEAN, admin);
        assertSuccess(getPermissionsAllowedNameAndActionsOnlyBean().writeNonBlocking(), WRITE_PERMISSION_BEAN, admin);
        assertSuccess(() -> getPermissionsAllowedNameAndActionsOnlyBean().read(), READ_PERMISSION_BEAN, user);
        assertSuccess(getPermissionsAllowedNameAndActionsOnlyBean().readNonBlocking(), READ_PERMISSION_BEAN, user);

        // identity has one permission and action, annotation has same permission and multiple actions
        assertSuccess(() -> getPermissionsAllowedNameAndActionsOnlyBean().multipleActions(), MULTIPLE_BEAN, user);
        assertSuccess(getPermissionsAllowedNameAndActionsOnlyBean().multipleNonBlockingActions(), MULTIPLE_BEAN, user);

        // identity has one permission and action, annotation has same permission and different actions
        final var user1 = new AuthData(Collections.singleton("user"), false, "user",
                permission(READ_PERMISSION, "diff1", "diff2"));
        assertFailureFor(() -> getPermissionsAllowedNameAndActionsOnlyBean().multipleActions(),
                ForbiddenException.class, user1);
        assertFailureFor(getPermissionsAllowedNameAndActionsOnlyBean().multipleNonBlockingActions(),
                ForbiddenException.class, user1);

        // identity has one permission and action, annotation has different permission and action
        assertFailureFor(() -> getPermissionsAllowedNameAndActionsOnlyBean().prohibited(), ForbiddenException.class,
                admin);
        assertFailureFor(getPermissionsAllowedNameAndActionsOnlyBean().prohibitedNonBlocking(),
                ForbiddenException.class, admin);

        // identity has no permission, annotation has one permission and action
        assertFailureFor(() -> getPermissionsAllowedNameAndActionsOnlyBean().prohibited(), UnauthorizedException.class,
                ANONYMOUS);
        assertFailureFor(getPermissionsAllowedNameAndActionsOnlyBean().prohibitedNonBlocking(),
                UnauthorizedException.class, ANONYMOUS);

        // identity has no permission, annotation has multiple permissions, each with one action
        assertFailureFor(() -> getPermissionsAllowedNameAndActionsOnlyBean().multiple(), UnauthorizedException.class,
                ANONYMOUS);
        assertFailureFor(getPermissionsAllowedNameAndActionsOnlyBean().multipleNonBlocking(),
                UnauthorizedException.class, ANONYMOUS);

        // identity has one permission and action, annotation has multiple permissions, each with one action
        assertSuccess(() -> getPermissionsAllowedNameAndActionsOnlyBean().multiple(), MULTIPLE_BEAN, user);
        assertSuccess(getPermissionsAllowedNameAndActionsOnlyBean().multipleNonBlocking(), MULTIPLE_BEAN, user);

        // identity has one permission and action, annotation has multiple permissions, some with actions, some not
        // should succeed as identity has 'read:bean', while annotation requires 'read'
        assertSuccess(() -> getPermissionsAllowedNameAndActionsOnlyBean().combination(), MULTIPLE_BEAN, user);
        assertSuccess(getPermissionsAllowedNameAndActionsOnlyBean().combinationNonBlockingActions(), MULTIPLE_BEAN,
                user);

        // identity has one permission and action, annotation has multiple permissions, some with actions, some not
        // should fail as identity has 'read:bean', while annotation requires 'read:meal' or 'read:bread'
        assertFailureFor(() -> getPermissionsAllowedNameAndActionsOnlyBean().combination2(), ForbiddenException.class,
                user);
        assertFailureFor(getPermissionsAllowedNameAndActionsOnlyBean().combination2NonBlockingActions(),
                ForbiddenException.class, user);

        // identity has one permission and action, annotation has different permissions, each with one action
        assertFailureFor(() -> getPermissionsAllowedNameAndActionsOnlyBean().multiple(), ForbiddenException.class,
                admin);
        assertFailureFor(getPermissionsAllowedNameAndActionsOnlyBean().multipleNonBlocking(), ForbiddenException.class,
                admin);

        // identity has one permission and no action, annotation has different permissions, each with one action
        assertFailureFor(() -> getPermissionsAllowedNameAndActionsOnlyBean().multiple(), ForbiddenException.class,
                USER);
        assertFailureFor(getPermissionsAllowedNameAndActionsOnlyBean().multipleNonBlocking(), ForbiddenException.class,
                USER);

        // identity has one permission and action, annotation has different permissions with multiple actions
        assertFailureFor(() -> getPermissionsAllowedNameAndActionsOnlyBean().multipleActions(),
                ForbiddenException.class, admin);
        assertFailureFor(getPermissionsAllowedNameAndActionsOnlyBean().multipleNonBlockingActions(),
                ForbiddenException.class, admin);

        // identity has one permission and no action, annotation has different permissions with multiple actions
        assertFailureFor(() -> getPermissionsAllowedNameAndActionsOnlyBean().multipleActions(),
                ForbiddenException.class, USER);
        assertFailureFor(getPermissionsAllowedNameAndActionsOnlyBean().multipleNonBlockingActions(),
                ForbiddenException.class, USER);

        // identity has one permission and one action, annotation has different permissions with multiple actions
        // most notably, annotation has "read:bread", "read:meal" and identity has "read:bread" => success
        final var readBread = new AuthData(Collections.singleton("admin"), false, "admin", permission("read", "bread"));
        assertSuccess(() -> getPermissionsAllowedNameAndActionsOnlyBean().combination2(), "combination2", readBread);
        assertSuccess(getPermissionsAllowedNameAndActionsOnlyBean().combination2NonBlockingActions(), "combination2",
                readBread);
    }

    @Test
    public void inclusivePermissionChecks() {
        // identity has one permission and action, annotation has multiple permissions, some with actions, some not
        // should fail as identity has 'read:bean', while all of "one", "two", "three:c", "three", "read:bread",
        // "read:meal" are required
        final var user = new AuthData(Collections.singleton("user"), false, "user",
                permission(READ_PERMISSION, "bean"));
        assertFailureFor(() -> getPermissionsAllowedNameAndActionsOnlyBean().inclusive(), ForbiddenException.class,
                user);
        assertFailureFor(getPermissionsAllowedNameAndActionsOnlyBean().inclusiveNonBlocking(), ForbiddenException.class,
                user);

        // should succeed as the identity has all required permissions and one more ('apple')
        var permissions = permissions("one", "two", "three", "apple");
        permissions.addAll(permission("three", "c"));
        permissions.addAll(permission("read", "bread", "meal"));
        final var user1 = new AuthData(Collections.singleton("user"), false, "user", permissions);
        assertSuccess(() -> getPermissionsAllowedNameAndActionsOnlyBean().inclusive(), new User("Martin"), user1);
        assertSuccess(getPermissionsAllowedNameAndActionsOnlyBean().inclusiveNonBlocking(), new User("Bruno"), user1);

        // should fail as the identity has all the permissions but one of required action si different
        // (has 'three:d' instead of 'three:c')
        permissions = permissions("one", "two", "three", "apple");
        permissions.addAll(permission("three", "d"));
        permissions.addAll(permission("read", "bread"));
        permissions.addAll(permission("read", "meal"));
        final var user2 = new AuthData(Collections.singleton("user"), false, "user", permissions);
        assertFailureFor(() -> getPermissionsAllowedNameAndActionsOnlyBean().inclusive(), ForbiddenException.class,
                user2);
        assertFailureFor(getPermissionsAllowedNameAndActionsOnlyBean().inclusiveNonBlocking(), ForbiddenException.class,
                user2);
    }

    @Test
    public void shouldRequireMultiplePermissions() {
        final var admin = new AuthData(Collections.singleton("admin"), false, "admin", permissions("create", "update"));
        final var user = new AuthData(Collections.singleton("user"), false, "user", permissions("see", "view"));

        // identity has 2 permissions, annotation requires 2 same permissions
        assertSuccess(() -> getMultiplePermissionsAllowedBean().createOrUpdate(), "create_or_update", admin);
        assertSuccess(getMultiplePermissionsAllowedBean().createOrUpdateNonBlocking(), "create_or_update", admin);
        assertSuccess(() -> getMultiplePermissionsAllowedBean().getOne(), "see_or_view_detail", user);
        assertSuccess(getMultiplePermissionsAllowedBean().getOneNonBlocking(), "see_or_view_detail", user);

        // identity has 2 permissions, annotation requires 2 different permissions
        assertFailureFor(() -> getMultiplePermissionsAllowedBean().createOrUpdate(), ForbiddenException.class, user);
        assertFailureFor(getMultiplePermissionsAllowedBean().createOrUpdateNonBlocking(), ForbiddenException.class,
                user);
        assertFailureFor(() -> getMultiplePermissionsAllowedBean().getOne(), ForbiddenException.class, admin);
        assertFailureFor(getMultiplePermissionsAllowedBean().getOneNonBlocking(), ForbiddenException.class, admin);

        final var create = new AuthData(Collections.singleton("create"), false, "create", permissions("create"));
        final var update = new AuthData(Collections.singleton("update"), false, "update", permissions("update"));
        final var see = new AuthData(Collections.singleton("see"), false, "see", permissions("see"));
        final var view = new AuthData(Collections.singleton("view"), false, "view", permissions("see"));

        // identity has 1 permissions, annotation requires 2 permissions, one of them is same as the identity possess
        assertFailureFor(() -> getMultiplePermissionsAllowedBean().createOrUpdate(), ForbiddenException.class, update);
        assertFailureFor(getMultiplePermissionsAllowedBean().createOrUpdateNonBlocking(), ForbiddenException.class,
                create);
        assertFailureFor(() -> getMultiplePermissionsAllowedBean().getOne(), ForbiddenException.class, see);
        assertFailureFor(getMultiplePermissionsAllowedBean().getOneNonBlocking(), ForbiddenException.class, view);

        // identity has 1 permissions, annotation requires 2 different permissions
        assertFailureFor(() -> getMultiplePermissionsAllowedBean().createOrUpdate(), ForbiddenException.class, see);
        assertFailureFor(getMultiplePermissionsAllowedBean().createOrUpdateNonBlocking(), ForbiddenException.class,
                view);
        assertFailureFor(() -> getMultiplePermissionsAllowedBean().getOne(), ForbiddenException.class, create);
        assertFailureFor(getMultiplePermissionsAllowedBean().getOneNonBlocking(), ForbiddenException.class, update);

        // (operand1 OR operand2 OR operand3) AND (operand4 OR operand5) AND (operand6 OR operand7) AND operand8

        // - identity posses no matching
        assertFailureFor(() -> getMultiplePermissionsAllowedBean().predicate(), ForbiddenException.class, admin);
        assertFailureFor(getMultiplePermissionsAllowedBean().predicateNonBlocking(), ForbiddenException.class, admin);

        // - identity has operand1 AND operand4 AND operand6 AND operand8 => success
        final var operand1_4_6_8 = new AuthData(Collections.singleton("operand1_4_6_8"), false, "operand1_4_6_8",
                permissions("operand1", "operand4", "operand6", "operand8"));
        assertSuccess(() -> getMultiplePermissionsAllowedBean().predicate(), PREDICATE, operand1_4_6_8);
        assertSuccess(getMultiplePermissionsAllowedBean().predicateNonBlocking(), PREDICATE, operand1_4_6_8);

        // - identity has operand3 AND operand5 AND operand7 AND operand8 => success
        final var operand3_5_7_8 = new AuthData(Collections.singleton("operand3_5_7_8"), false, "operand3_5_7_8",
                permissions("operand3", "operand5", "operand7", "operand8"));
        assertSuccess(() -> getMultiplePermissionsAllowedBean().predicate(), PREDICATE, operand3_5_7_8);
        assertSuccess(getMultiplePermissionsAllowedBean().predicateNonBlocking(), PREDICATE, operand3_5_7_8);

        // - identity has operand1 AND operand4 AND operand6 => missing operand8 => failure
        final var operand1_4_6 = new AuthData(Collections.singleton("operand1_4_6"), false, "operand1_4_6",
                permissions("operand1", "operand4", "operand6"));
        assertFailureFor(() -> getMultiplePermissionsAllowedBean().predicate(), ForbiddenException.class, operand1_4_6);
        assertFailureFor(getMultiplePermissionsAllowedBean().predicateNonBlocking(), ForbiddenException.class,
                operand1_4_6);

        // - identity has operand1 AND operand4 AND operand8 => missing operand6 or operand7 => failure
        final var operand1_4_8 = new AuthData(Collections.singleton("operand1_4_8"), false, "operand1_4_8",
                permissions("operand1", "operand4", "operand8"));
        assertFailureFor(() -> getMultiplePermissionsAllowedBean().predicate(), ForbiddenException.class, operand1_4_8);
        assertFailureFor(getMultiplePermissionsAllowedBean().predicateNonBlocking(), ForbiddenException.class,
                operand1_4_8);

        // - identity has operand1 AND operand6 AND operand8 => missing operand4 or operand5 => failure
        final var operand1_6_8 = new AuthData(Collections.singleton("operand1_6_8"), false, "operand1_6_8",
                permissions("operand1", "operand6", "operand8"));
        assertFailureFor(() -> getMultiplePermissionsAllowedBean().predicate(), ForbiddenException.class, operand1_6_8);
        assertFailureFor(getMultiplePermissionsAllowedBean().predicateNonBlocking(), ForbiddenException.class,
                operand1_6_8);

        // - identity has operand4 AND operand6 AND operand8 => missing operand1 or operand2 or operand3 => failure
        final var operand4_6_8 = new AuthData(Collections.singleton("operand4_6_8"), false, "operand4_6_8",
                permissions("operand4", "operand6", "operand8"));
        assertFailureFor(() -> getMultiplePermissionsAllowedBean().predicate(), ForbiddenException.class, operand4_6_8);
        assertFailureFor(getMultiplePermissionsAllowedBean().predicateNonBlocking(), ForbiddenException.class,
                operand4_6_8);

        // - identity has all operands => success
        final var full_operands = new AuthData(Collections.singleton("full_operands"), false, "full_operands",
                permissions("operand1", "operand2", "operand3", "operand4", "operand5", "operand6", "operand7",
                        "operand8"));
        assertSuccess(() -> getMultiplePermissionsAllowedBean().predicate(), PREDICATE, full_operands);
        assertSuccess(getMultiplePermissionsAllowedBean().predicateNonBlocking(), PREDICATE, full_operands);
    }

    @Test
    public void shouldRequireMultiplePermissionsWithActions() {
        // (permission1:action1 OR permission2:action2) AND (permission1:action2 OR permission2:action1)
        // - success
        // - permission1:action1, permission2:action2, permission1:action2, permission2:action1
        assertSuccess(() -> getMultiplePermissionsAllowedBean().actionsPredicate(), PREDICATE,
                withPermissionsAndActions(1, 1, 2, 2, 1, 2, 2, 1));
        assertSuccess(getMultiplePermissionsAllowedBean().actionsPredicateNonBlocking(), PREDICATE,
                withPermissionsAndActions(1, 1, 2, 2, 1, 2, 2, 1));
        // - permission1:action1, permission1:action2, permission2:action1
        assertSuccess(() -> getMultiplePermissionsAllowedBean().actionsPredicate(), PREDICATE,
                withPermissionsAndActions(1, 1, 1, 2, 2, 1));
        assertSuccess(getMultiplePermissionsAllowedBean().actionsPredicateNonBlocking(), PREDICATE,
                withPermissionsAndActions(1, 1, 1, 2, 2, 1));
        // - permission2:action2, permission1:action2, permission2:action1
        assertSuccess(() -> getMultiplePermissionsAllowedBean().actionsPredicate(), PREDICATE,
                withPermissionsAndActions(2, 2, 1, 2, 2, 1));
        assertSuccess(getMultiplePermissionsAllowedBean().actionsPredicateNonBlocking(), PREDICATE,
                withPermissionsAndActions(2, 2, 1, 2, 2, 1));
        // - permission1:action1, permission2:action2, permission1:action2
        assertSuccess(() -> getMultiplePermissionsAllowedBean().actionsPredicate(), PREDICATE,
                withPermissionsAndActions(1, 1, 2, 2, 1, 2));
        assertSuccess(getMultiplePermissionsAllowedBean().actionsPredicateNonBlocking(), PREDICATE,
                withPermissionsAndActions(1, 1, 2, 2, 1, 2));
        // - permission1:action1, permission2:action2, permission2:action1
        assertSuccess(() -> getMultiplePermissionsAllowedBean().actionsPredicate(), PREDICATE,
                withPermissionsAndActions(1, 1, 2, 2, 2, 1));
        assertSuccess(getMultiplePermissionsAllowedBean().actionsPredicateNonBlocking(), PREDICATE,
                withPermissionsAndActions(1, 1, 2, 2, 2, 1));
        // - permission2:action2, permission2:action1
        assertSuccess(() -> getMultiplePermissionsAllowedBean().actionsPredicate(), PREDICATE,
                withPermissionsAndActions(2, 2, 2, 1));
        assertSuccess(getMultiplePermissionsAllowedBean().actionsPredicateNonBlocking(), PREDICATE,
                withPermissionsAndActions(2, 2, 2, 1));
        // - permission2:action2, permission1:action2
        assertSuccess(() -> getMultiplePermissionsAllowedBean().actionsPredicate(), PREDICATE,
                withPermissionsAndActions(2, 2, 1, 2));
        assertSuccess(getMultiplePermissionsAllowedBean().actionsPredicateNonBlocking(), PREDICATE,
                withPermissionsAndActions(2, 2, 1, 2));
        // - permission1:action1, permission2:action1
        assertSuccess(() -> getMultiplePermissionsAllowedBean().actionsPredicate(), PREDICATE,
                withPermissionsAndActions(1, 1, 2, 1));
        assertSuccess(getMultiplePermissionsAllowedBean().actionsPredicateNonBlocking(), PREDICATE,
                withPermissionsAndActions(1, 1, 2, 1));
        // - permission1:action1, permission1:action2
        assertSuccess(() -> getMultiplePermissionsAllowedBean().actionsPredicate(), PREDICATE,
                withPermissionsAndActions(1, 1, 1, 2));
        assertSuccess(getMultiplePermissionsAllowedBean().actionsPredicateNonBlocking(), PREDICATE,
                withPermissionsAndActions(1, 1, 1, 2));
        // - failure
        // - permission1:action1, permission2:action2
        assertFailureFor(() -> getMultiplePermissionsAllowedBean().actionsPredicate(), ForbiddenException.class,
                withPermissionsAndActions(1, 1, 2, 2));
        assertFailureFor(getMultiplePermissionsAllowedBean().actionsPredicateNonBlocking(), ForbiddenException.class,
                withPermissionsAndActions(1, 1, 2, 2));
        // - permission1:action2, permission2:action1
        assertFailureFor(() -> getMultiplePermissionsAllowedBean().actionsPredicate(), ForbiddenException.class,
                withPermissionsAndActions(1, 2, 2, 1));
        assertFailureFor(getMultiplePermissionsAllowedBean().actionsPredicateNonBlocking(), ForbiddenException.class,
                withPermissionsAndActions(1, 2, 2, 1));
        // - permission1:action2
        assertFailureFor(() -> getMultiplePermissionsAllowedBean().actionsPredicate(), ForbiddenException.class,
                withPermissionsAndActions(1, 2));
        assertFailureFor(getMultiplePermissionsAllowedBean().actionsPredicateNonBlocking(), ForbiddenException.class,
                withPermissionsAndActions(1, 2));
        // - permission2:action1
        assertFailureFor(() -> getMultiplePermissionsAllowedBean().actionsPredicate(), ForbiddenException.class,
                withPermissionsAndActions(2, 1));
        assertFailureFor(getMultiplePermissionsAllowedBean().actionsPredicateNonBlocking(), ForbiddenException.class,
                withPermissionsAndActions(2, 1));
        // - permission2, permission1, permission2
        var permissionsOnlyIdentity = new AuthData(Collections.singleton("permissions_actions"), false,
                "permissions_actions", permissions("permission2", "permission1", "permission2"));
        assertFailureFor(() -> getMultiplePermissionsAllowedBean().actionsPredicate(), ForbiddenException.class,
                permissionsOnlyIdentity);
        assertFailureFor(getMultiplePermissionsAllowedBean().actionsPredicateNonBlocking(), ForbiddenException.class,
                permissionsOnlyIdentity);
        // - no matching permissions
        var noMatchIdentity = new AuthData(Collections.singleton("permissions_actions"), false, "permissions_actions",
                permissions("no_match"));
        assertFailureFor(() -> getMultiplePermissionsAllowedBean().actionsPredicate(), ForbiddenException.class,
                noMatchIdentity);
        assertFailureFor(getMultiplePermissionsAllowedBean().actionsPredicateNonBlocking(), ForbiddenException.class,
                noMatchIdentity);
    }

    @Test
    public void inclusivePermissionChecksRepeatable() {
        // (permission1:action1 AND permission2:action2) AND (permission1:action2 AND permission2:action1)
        // - success
        // - permission1:action1, permission2:action2, permission1:action2, permission2:action1
        assertSuccess(() -> getMultiplePermissionsAllowedBean().inclusive(), new User("Sergey"),
                withPermissionsAndActions(1, 1, 2, 2, 1, 2, 2, 1));
        assertSuccess(getMultiplePermissionsAllowedBean().inclusiveNonBlocking(), new User("Stuart"),
                withPermissionsAndActions(1, 1, 2, 2, 1, 2, 2, 1));
        // - failure
        // - permission1:action1, permission2:action2, permission1:action2
        assertFailureFor(() -> getMultiplePermissionsAllowedBean().inclusive(), ForbiddenException.class,
                withPermissionsAndActions(1, 1, 2, 2, 1, 2));
        assertFailureFor(getMultiplePermissionsAllowedBean().inclusiveNonBlocking(), ForbiddenException.class,
                withPermissionsAndActions(1, 1, 2, 2, 1, 2));
        // - permission1:action1, permission2:action2, permission2:action1
        assertFailureFor(() -> getMultiplePermissionsAllowedBean().inclusive(), ForbiddenException.class,
                withPermissionsAndActions(1, 1, 2, 2, 2, 1));
        assertFailureFor(getMultiplePermissionsAllowedBean().inclusiveNonBlocking(), ForbiddenException.class,
                withPermissionsAndActions(1, 1, 2, 2, 2, 1));
        // - permission1:action1, permission2:action2
        assertFailureFor(() -> getMultiplePermissionsAllowedBean().inclusive(), ForbiddenException.class,
                withPermissionsAndActions(1, 1, 2, 2));
        assertFailureFor(getMultiplePermissionsAllowedBean().inclusiveNonBlocking(), ForbiddenException.class,
                withPermissionsAndActions(1, 1, 2, 2));
        // - permission1:action1
        assertFailureFor(() -> getMultiplePermissionsAllowedBean().inclusive(), ForbiddenException.class,
                withPermissionsAndActions(1, 1));
        assertFailureFor(getMultiplePermissionsAllowedBean().inclusiveNonBlocking(), ForbiddenException.class,
                withPermissionsAndActions(1, 1));
        // - no matching permission
        var noMatchIdentity = new AuthData(Collections.singleton("permissions_actions"), false, "permissions_actions",
                permissions("no_match"));
        assertFailureFor(() -> getMultiplePermissionsAllowedBean().inclusive(), ForbiddenException.class,
                noMatchIdentity);
        assertFailureFor(getMultiplePermissionsAllowedBean().inclusiveNonBlocking(), ForbiddenException.class,
                noMatchIdentity);
    }

    protected abstract MultiplePermissionsAllowedBeanI getMultiplePermissionsAllowedBean();

    protected abstract PermissionsAllowedNameOnlyBeanI getPermissionsAllowedNameOnlyBean();

    protected abstract PermissionsAllowedNameAndActionsOnlyBeanI getPermissionsAllowedNameAndActionsOnlyBean();

    protected abstract Permission createPermission(String name, String... actions);

    AuthData withPermissionsAndActions(int... ixs) {
        var permissions = new HashSet<Permission>();
        String name = null;
        for (int ix : ixs) {
            if (name == null) {
                name = "permission" + ix;
            } else {
                permissions.add(createPermission(name, "action" + ix));
                name = null;
            }
        }
        return new AuthData(Collections.singleton("permissions_actions"), false, "permissions_actions", permissions);
    }

    Set<Permission> permissions(String... permissionNames) {
        var permissions = new HashSet<Permission>();
        for (String permissionName : permissionNames) {
            permissions.add(createPermission(permissionName));
        }
        return permissions;
    }

    Set<Permission> permission(String permissionName, String... actions) {
        return Set.of(createPermission(permissionName, actions));
    }

    public interface PermissionsAllowedNameOnlyBeanI {
        String write();

        String read();

        Uni<String> writeNonBlocking();

        Uni<String> readNonBlocking();

        void prohibited();

        Uni<Void> prohibitedNonBlocking();

        String multiple();

        Uni<String> multipleNonBlocking();
    }

    public interface PermissionsAllowedNameAndActionsOnlyBeanI {
        String write();

        String read();

        Uni<String> writeNonBlocking();

        Uni<String> readNonBlocking();

        void prohibited();

        Uni<Void> prohibitedNonBlocking();

        String multiple();

        Uni<String> multipleNonBlocking();

        String multipleActions();

        Uni<String> multipleNonBlockingActions();

        String combination();

        Uni<String> combinationNonBlockingActions();

        String combination2();

        Uni<String> combination2NonBlockingActions();

        User inclusive();

        Uni<User> inclusiveNonBlocking();
    }

    public interface MultiplePermissionsAllowedBeanI {
        String createOrUpdate();

        Uni<String> createOrUpdateNonBlocking();

        String getOne();

        Uni<String> getOneNonBlocking();

        Uni<String> predicateNonBlocking();

        String predicate();

        String actionsPredicate();

        Uni<String> actionsPredicateNonBlocking();

        User inclusive();

        Uni<User> inclusiveNonBlocking();
    }

    protected static class User {

        protected final String name;

        protected User(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            User user = (User) o;
            return name.equals(user.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

}
