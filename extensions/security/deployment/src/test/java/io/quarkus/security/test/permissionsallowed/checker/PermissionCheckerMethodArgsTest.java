package io.quarkus.security.test.permissionsallowed.checker;

import static io.quarkus.security.test.utils.IdentityMock.ADMIN;
import static io.quarkus.security.test.utils.IdentityMock.USER;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class PermissionCheckerMethodArgsTest {

    private static final AuthData USER_WITH_AUGMENTORS = new AuthData(USER, true);
    private static final AuthData ADMIN_WITH_AUGMENTORS = new AuthData(ADMIN, true);

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(IdentityMock.class, AuthData.class, SecurityTestUtils.class,
                            PermissionCheckerOnlySecurityIdentity.class,
                            PermissionCheckerOneSecuredMethodArg.class, PermissionCheckerTwoSecuredMethodArgs.class,
                            PermissionCheckerThreeSecuredMethodArgs.class,
                            PermissionCheckerFourSecuredMethodArgs.class, AbstractNthMethodArgChecker.class,
                            PermissionChecker1stMethodArg.class, PermissionChecker2ndMethodArg.class,
                            PermissionChecker3rdMethodArg.class, PermissionChecker4thMethodArg.class,
                            PermissionChecker5thMethodArg.class, PermissionChecker6thMethodArg.class,
                            PermissionChecker7thMethodArg.class));

    @Inject
    MethodArgsBean bean;

    @Test
    public void testOnlySecurityIdentityCheckerArg() {
        assertSuccess(() -> bean.zeroSecuredMethodArguments(), "zeroSecuredMethodArguments", ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.zeroSecuredMethodArguments(), ForbiddenException.class, ADMIN);
        assertFailureFor(() -> bean.zeroSecuredMethodArguments(), ForbiddenException.class, USER_WITH_AUGMENTORS);

        assertSuccess(() -> bean.oneSecuredMethodArgument_2(1), "oneSecuredMethodArgument_2", ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.oneSecuredMethodArgument_2(1), ForbiddenException.class, USER_WITH_AUGMENTORS);
    }

    @Test
    public void testOneCheckerArgument() {
        assertSuccess(() -> bean.oneSecuredMethodArgument("1"), "oneSecuredMethodArgument", ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.oneSecuredMethodArgument("1"), ForbiddenException.class, ADMIN);
        assertFailureFor(() -> bean.oneSecuredMethodArgument("1"), ForbiddenException.class, USER_WITH_AUGMENTORS);

        assertSuccess(() -> bean.twoSecuredMethodArgument_2(1, 2), "twoSecuredMethodArgument_2", ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.twoSecuredMethodArgument_2(1, 2), ForbiddenException.class, USER_WITH_AUGMENTORS);

        // wrong value of 'one'
        assertFailureFor(() -> bean.twoSecuredMethodArgument_2(9, 2), ForbiddenException.class, ADMIN_WITH_AUGMENTORS);
    }

    @Test
    public void testTwoCheckerArguments() {
        assertSuccess(() -> bean.twoSecuredMethodArgument(1, 2), "twoSecuredMethodArgument", ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.twoSecuredMethodArgument(1, 2), ForbiddenException.class, ADMIN);
        assertFailureFor(() -> bean.twoSecuredMethodArgument(1, 2), ForbiddenException.class, USER_WITH_AUGMENTORS);

        assertSuccess(() -> bean.threeSecuredMethodArguments_2(1, 2, "3"), "threeSecuredMethodArguments_2",
                ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.threeSecuredMethodArguments_2(1, 2, "3"), ForbiddenException.class, USER_WITH_AUGMENTORS);

        // wrong value of 'two'
        assertFailureFor(() -> bean.threeSecuredMethodArguments_2(1, 4, "3"), ForbiddenException.class, ADMIN_WITH_AUGMENTORS);
    }

    @Test
    public void testThreeCheckerArguments() {
        assertSuccess(() -> bean.threeSecuredMethodArguments("1", "2", "3"), "threeSecuredMethodArguments",
                ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.threeSecuredMethodArguments("1", "2", "3"), ForbiddenException.class, ADMIN);
        assertFailureFor(() -> bean.threeSecuredMethodArguments("1", "2", "3"), ForbiddenException.class, USER_WITH_AUGMENTORS);

        assertSuccess(() -> bean.fourSecuredMethodArguments_2("1", 2, "3", 4), "fourSecuredMethodArguments_2",
                ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.fourSecuredMethodArguments_2("1", 2, "3", 4), ForbiddenException.class,
                USER_WITH_AUGMENTORS);

        // wrong value of 'one'
        assertFailureFor(() -> bean.fourSecuredMethodArguments_2("987", 2, "3", 4), ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);
    }

    @Test
    public void testFourCheckerArguments() {
        assertSuccess(() -> bean.fourSecuredMethodArguments(1, 2, "3", "4"), "fourSecuredMethodArguments",
                ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.fourSecuredMethodArguments(1, 2, "3", "4"), ForbiddenException.class, ADMIN);
        assertFailureFor(() -> bean.fourSecuredMethodArguments(1, 2, "3", "4"), ForbiddenException.class, USER_WITH_AUGMENTORS);

        assertSuccess(() -> bean.fiveSecuredMethodArguments_2("1", 2, "3", 4, "5"), "fiveSecuredMethodArguments_2",
                ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.fiveSecuredMethodArguments_2("1", 2, "3", 4, "5"), ForbiddenException.class,
                USER_WITH_AUGMENTORS);

        // wrong value of 'four'
        assertFailureFor(() -> bean.fiveSecuredMethodArguments_2("1", 2, "3", 8, "5"), ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);
    }

    @Test
    public void testMultipleCheckersForOneSecMethod_annotationRepeated() {
        // === all 5 arguments required by 5 different permissions
        assertSuccess(() -> bean.fiveSecuredMethodArguments(1, 2, "3", "4", "5"), "fiveSecuredMethodArguments",
                ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.fiveSecuredMethodArguments(1, 2, "3", "4", "5"), ForbiddenException.class, ADMIN);
        assertFailureFor(() -> bean.fiveSecuredMethodArguments(1, 2, "3", "4", "5"), ForbiddenException.class,
                USER_WITH_AUGMENTORS);
        // 1st is wrong
        assertFailureFor(() -> bean.fiveSecuredMethodArguments(7, 2, "3", "4", "5"), ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);
        // 2nd is wrong
        assertFailureFor(() -> bean.fiveSecuredMethodArguments(1, 3, "3", "4", "5"), ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);
        // 3rd is wrong
        assertFailureFor(() -> bean.fiveSecuredMethodArguments(1, 2, "2", "4", "5"), ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);
        // 4th is wrong
        assertFailureFor(() -> bean.fiveSecuredMethodArguments(1, 2, "3", "5", "5"), ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);
        // 5th is wrong
        assertFailureFor(() -> bean.fiveSecuredMethodArguments(1, 2, "3", "4", "6"), ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);

        // === all 6 arguments required by 6 different permissions
        assertSuccess(() -> bean.sixSecuredMethodArguments_2("1", 2, "3", 4, "5", 6), "sixSecuredMethodArguments_2",
                ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.sixSecuredMethodArguments_2("1", 2, "3", 4, "5", 6), ForbiddenException.class,
                USER_WITH_AUGMENTORS);
        // 5th is wrong
        assertFailureFor(() -> bean.sixSecuredMethodArguments_2("1", 2, "3", 4, "6", 6), ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);
        // 6th is wrong
        assertFailureFor(() -> bean.sixSecuredMethodArguments_2("1", 2, "3", 4, "5", 7), ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);

        // === 5th and 6th argument required by 2 different permissions
        assertSuccess(() -> bean.sixSecuredMethodArguments(1, 2, "3", "4", "5", 6), "sixSecuredMethodArguments",
                ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.sixSecuredMethodArguments(1, 2, "3", "4", "5", 6), ForbiddenException.class,
                USER_WITH_AUGMENTORS);
        // 5th is wrong
        assertFailureFor(() -> bean.sixSecuredMethodArguments(1, 2, "3", "4", "6", 6), ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);
        // 6th is wrong
        assertFailureFor(() -> bean.sixSecuredMethodArguments(1, 2, "3", "4", "5", 7), ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);

        // === all 7 arguments required by 7 different permissions
        assertSuccess(() -> bean.sevenSecuredMethodArguments_2("1", 2, "3", 4, "5", 6, 7), "sevenSecuredMethodArguments_2",
                ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.sevenSecuredMethodArguments_2("1", 2, "3", 4, "5", 6, 7), ForbiddenException.class,
                USER_WITH_AUGMENTORS);
        // 5th is wrong
        assertFailureFor(() -> bean.sevenSecuredMethodArguments_2("1", 2, "3", 4, "5", 5, 7), ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);
        // 6th is wrong
        assertFailureFor(() -> bean.sevenSecuredMethodArguments_2("1", 2, "3", 4, "5", 6, 8), ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);

        // === 6th and 7th argument required by 2 different permissions
        assertSuccess(() -> bean.sevenSecuredMethodArguments(1, 2, "3", "4", "5", 6, 7), "sevenSecuredMethodArguments",
                ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.sevenSecuredMethodArguments(1, 2, "3", "4", "5", 6, 7), ForbiddenException.class,
                USER_WITH_AUGMENTORS);
        // 1st is wrong
        assertFailureFor(() -> bean.sevenSecuredMethodArguments(0, 2, "3", "4", "5", 6, 7), ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);
        // 7th is wrong
        assertFailureFor(() -> bean.sevenSecuredMethodArguments(1, 2, "3", "4", "5", 6, 8), ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);
    }

    @Test
    public void testMultipleCheckersForOneSecMethod_inclusive() {
        // === all 7 arguments required by 7 different permissions inside single @PermissionsAllowed annotation instance
        // another 2 permissions ("another-6th-arg", "another-7th-arg") are required by second @PermissionsAllowed instance
        // therefore user needs all 9 permissions
        assertSuccess(() -> bean.sevenSecuredMethodArguments_2_inclusive("1", 2, "3", 4, "5", 6, 7),
                "sevenSecuredMethodArguments_2_inclusive",
                ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.sevenSecuredMethodArguments_2_inclusive("1", 2, "3", 4, "5", 6, 7),
                ForbiddenException.class,
                USER_WITH_AUGMENTORS);
        // 5th is wrong
        assertFailureFor(() -> bean.sevenSecuredMethodArguments_2_inclusive("1", 2, "3", 4, "5", 5, 7),
                ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);
        // 6th is wrong
        assertFailureFor(() -> bean.sevenSecuredMethodArguments_2_inclusive("1", 2, "3", 4, "5", 6, 8),
                ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);
        // permission checker "another-6th-arg" accepts either int or String, but never long
        // pass: 6th param is string
        assertSuccess(() -> bean.sevenSecuredMethodArguments_2_inclusive("1", 2, "3", 4, "5", "6", 7),
                "sevenSecuredMethodArguments_2_inclusive",
                ADMIN_WITH_AUGMENTORS);
        // fail: 6th param is long
        assertFailureFor(() -> bean.sevenSecuredMethodArguments_2_inclusive("1", 2, "3", 4, "5", 6L, 7),
                ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);
        // permission checker "another-7th-arg" accepts either int or long, but never String
        // pass: 7th param is long
        assertSuccess(() -> bean.sevenSecuredMethodArguments_2_inclusive("1", 2, "3", 4, "5", 6, 7L),
                "sevenSecuredMethodArguments_2_inclusive",
                ADMIN_WITH_AUGMENTORS);
        // fail: 7th param is string
        assertFailureFor(() -> bean.sevenSecuredMethodArguments_2_inclusive("1", 2, "3", 4, "5", 6, "7"),
                ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);

        // === all 6 arguments required by 6 different permissions inside one @PermissionsAllowed annotation instance
        assertSuccess(() -> bean.sixSecuredMethodArguments_2("1", 2, "3", 4, "5", 6), "sixSecuredMethodArguments_2",
                ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> bean.sixSecuredMethodArguments_2("1", 2, "3", 4, "5", 6), ForbiddenException.class,
                USER_WITH_AUGMENTORS);
        // 5th is wrong
        assertFailureFor(() -> bean.sixSecuredMethodArguments_2("1", 2, "3", 4, "6", 6), ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);
        // 6th is wrong
        assertFailureFor(() -> bean.sixSecuredMethodArguments_2("1", 2, "3", 4, "5", 7), ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);
    }

    @ApplicationScoped
    public static class MethodArgsBean {

        @PermissionsAllowed("only-security-identity")
        public String zeroSecuredMethodArguments() {
            return "zeroSecuredMethodArguments";
        }

        @PermissionsAllowed("one-arg")
        public String oneSecuredMethodArgument(String one) {
            return "oneSecuredMethodArgument";
        }

        @PermissionsAllowed("only-security-identity")
        public String oneSecuredMethodArgument_2(int one) {
            return "oneSecuredMethodArgument_2";
        }

        @PermissionsAllowed("two-args")
        public String twoSecuredMethodArgument(long one, long two) {
            return "twoSecuredMethodArgument";
        }

        @PermissionsAllowed("one-arg")
        public String twoSecuredMethodArgument_2(long one, long two) {
            return "twoSecuredMethodArgument_2";
        }

        @PermissionsAllowed("three-args")
        public String threeSecuredMethodArguments(String one, String two, String three) {
            return "threeSecuredMethodArguments";
        }

        @PermissionsAllowed("two-args")
        public String threeSecuredMethodArguments_2(int one, int two, String three) {
            return "threeSecuredMethodArguments_2";
        }

        @PermissionsAllowed("four-args")
        public String fourSecuredMethodArguments(int one, int two, String three, String four) {
            return "fourSecuredMethodArguments";
        }

        @PermissionsAllowed("three-args")
        public String fourSecuredMethodArguments_2(String one, int two, String three, int four) {
            return "fourSecuredMethodArguments_2";
        }

        @PermissionsAllowed("1st-arg")
        @PermissionsAllowed("2nd-arg")
        @PermissionsAllowed("3rd-arg")
        @PermissionsAllowed("4th-arg")
        @PermissionsAllowed("5th-arg")
        public String fiveSecuredMethodArguments(int one, int two, String three, String four, String five) {
            return "fiveSecuredMethodArguments";
        }

        @PermissionsAllowed("four-args")
        public String fiveSecuredMethodArguments_2(String one, int two, String three, int four, String five) {
            return "fiveSecuredMethodArguments_2";
        }

        @PermissionsAllowed("5th-arg")
        @PermissionsAllowed("6th-arg")
        public String sixSecuredMethodArguments(int one, int two, String three, String four, String five, Object six) {
            return "sixSecuredMethodArguments";
        }

        @PermissionsAllowed("1st-arg")
        @PermissionsAllowed("2nd-arg")
        @PermissionsAllowed("3rd-arg")
        @PermissionsAllowed("4th-arg")
        @PermissionsAllowed("5th-arg")
        @PermissionsAllowed("6th-arg")
        public String sixSecuredMethodArguments_2(String one, int two, String three, int four, String five, Object six) {
            return "sixSecuredMethodArguments_2";
        }

        @PermissionsAllowed(value = { "1st-arg", "2nd-arg", "3rd-arg", "4th-arg", "5th-arg", "6th-arg" }, inclusive = true)
        public String sixSecuredMethodArguments_2_inclusive(String one, int two, String three, int four, String five,
                Object six) {
            return "sixSecuredMethodArguments_2_inclusive";
        }

        @PermissionsAllowed("1st-arg")
        @PermissionsAllowed("7th-arg")
        public String sevenSecuredMethodArguments(int one, int two, String three, String four, String five, Object six,
                Object seven) {
            return "sevenSecuredMethodArguments";
        }

        @PermissionsAllowed("1st-arg")
        @PermissionsAllowed("2nd-arg")
        @PermissionsAllowed("3rd-arg")
        @PermissionsAllowed("4th-arg")
        @PermissionsAllowed("5th-arg")
        @PermissionsAllowed("6th-arg")
        @PermissionsAllowed("7th-arg")
        public String sevenSecuredMethodArguments_2(String one, int two, String three, int four, String five, Object six,
                Object seven) {
            return "sevenSecuredMethodArguments_2";
        }

        @PermissionsAllowed(value = { "another-6th-arg", "another-7th-arg" }, inclusive = true)
        @PermissionsAllowed(value = { "1st-arg", "2nd-arg", "3rd-arg", "4th-arg", "5th-arg", "6th-arg",
                "7th-arg" }, inclusive = true)
        public String sevenSecuredMethodArguments_2_inclusive(String one, int two, String three, int four, String five,
                Object six,
                Object seven) {
            return "sevenSecuredMethodArguments_2_inclusive";
        }
    }
}
