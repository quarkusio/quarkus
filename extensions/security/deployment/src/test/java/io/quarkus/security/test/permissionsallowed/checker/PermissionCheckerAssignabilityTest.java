package io.quarkus.security.test.permissionsallowed.checker;

import static io.quarkus.security.test.utils.IdentityMock.ADMIN;
import static io.quarkus.security.test.utils.IdentityMock.USER;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.PermissionChecker;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class PermissionCheckerAssignabilityTest {

    private static final AuthData USER_WITH_AUGMENTORS = new AuthData(USER, true);
    private static final AuthData ADMIN_WITH_AUGMENTORS = new AuthData(ADMIN, true);

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(IdentityMock.class, AuthData.class, SecurityTestUtils.class));

    @Inject
    AssignabilitySecuredBean securedBean;

    @Test
    public void testAssignabilityFromTopLevelInterface() {
        var recordCorrectVal = new Second_Record("top");
        assertSuccess(() -> securedBean.top(recordCorrectVal), "top", ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> securedBean.top(recordCorrectVal), ForbiddenException.class, USER_WITH_AUGMENTORS);
        var recordWrongVal = new Second_Record("wrong-value");
        assertFailureFor(() -> securedBean.top(recordWrongVal), ForbiddenException.class, ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> securedBean.top(recordWrongVal), ForbiddenException.class, USER_WITH_AUGMENTORS);

        var classCorrectVal = new Third("top");
        assertSuccess(() -> securedBean.top(classCorrectVal), "top", ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> securedBean.top(classCorrectVal), ForbiddenException.class, USER_WITH_AUGMENTORS);
        var classWrongVal = new Third("wrong-value");
        assertFailureFor(() -> securedBean.top(classWrongVal), ForbiddenException.class, ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> securedBean.top(classWrongVal), ForbiddenException.class, USER_WITH_AUGMENTORS);
    }

    @Test
    public void testAssignabilityFromAbstractClass() {
        var classCorrectVal = new Third("abstract");
        assertSuccess(() -> securedBean.secondAbstract(classCorrectVal), "abstract", ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> securedBean.secondAbstract(classCorrectVal), ForbiddenException.class, USER_WITH_AUGMENTORS);
        var classWrongVal = new Third("wrong-value");
        assertFailureFor(() -> securedBean.secondAbstract(classWrongVal), ForbiddenException.class, ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> securedBean.secondAbstract(classWrongVal), ForbiddenException.class, USER_WITH_AUGMENTORS);
    }

    @Test
    public void testAssignabilityFromImplementation() {
        var classCorrectVal = new Third("class");
        assertSuccess(() -> securedBean.thirdImplementation(classCorrectVal), "class", ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> securedBean.thirdImplementation(classCorrectVal), ForbiddenException.class,
                USER_WITH_AUGMENTORS);
        var classWrongVal = new Third("wrong-value");
        assertFailureFor(() -> securedBean.thirdImplementation(classWrongVal), ForbiddenException.class, ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> securedBean.thirdImplementation(classWrongVal), ForbiddenException.class, USER_WITH_AUGMENTORS);
    }

    @Test
    public void testAllThreeLevels() {
        // secured method accepts interface, abstract class and implementation
        // checker accepts implementation thrice (once for each secured method param)
        var theInterface = new Third("interface");
        var theAbstract = new Third("abstract");
        var implementation = new Third("implementation");
        assertSuccess(() -> securedBean.allThree(implementation, theAbstract, theInterface), "allThree", ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> securedBean.allThree(theInterface, theAbstract, implementation), ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> securedBean.allThree(theAbstract, theInterface, implementation), ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);
    }

    @Test
    public void testGenericChecker() {
        var wrongValue = new Third("wrong-value");
        var rightValue = new Third("generic");
        var anotherRightValue = new Second_Record("generic");
        assertSuccess(() -> securedBean.genericChecker(rightValue), "generic", ADMIN_WITH_AUGMENTORS);
        assertSuccess(() -> securedBean.genericChecker(anotherRightValue), "generic", ADMIN_WITH_AUGMENTORS);
        assertFailureFor(() -> securedBean.genericChecker(wrongValue), ForbiddenException.class, ADMIN_WITH_AUGMENTORS);
    }

    @Test
    public void testNotAssignableCheckerParam() {
        var theInterface = new Second_Record("interface"); // not assignable
        var theAbstract = new Third("abstract");
        var implementation = new Third("implementation");
        assertFailureFor(() -> securedBean.allThree(implementation, theAbstract, theInterface), ForbiddenException.class,
                ADMIN_WITH_AUGMENTORS);
    }

    @ApplicationScoped
    public static class AssignabilitySecuredBean {

        @PermissionsAllowed("top")
        String top(Top top) {
            return top.value();
        }

        @PermissionsAllowed("abstract")
        String secondAbstract(Second_Abstract secondAbstract) {
            return secondAbstract.value();
        }

        @PermissionsAllowed("class")
        String thirdImplementation(Third third) {
            return third.value();
        }

        @PermissionsAllowed("thriceThird")
        String allThree(Third implementation, Second_Abstract theAbstract, Top theInterface) {
            return "allThree";
        }

        @PermissionsAllowed("generic-checker")
        String genericChecker(Top top) {
            return top.value();
        }
    }

    interface Top {

        String value();

    }

    record Second_Record(String value) implements Top {
    }

    static abstract class Second_Abstract implements Top {
    }

    static class Third extends Second_Abstract {

        private final String value;

        Third(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    @Singleton
    static class Checkers {

        @Inject
        SecurityIdentity identity;

        @PermissionChecker("top")
        boolean isTopAllowed(Top top) {
            return identity.hasRole("admin") && top.value().equals("top");
        }

        @PermissionChecker("abstract")
        boolean isAbstractAllowed(Second_Abstract secondAbstract) {
            return identity.hasRole("admin") && secondAbstract.value().equals("abstract");
        }

        @PermissionChecker("class")
        boolean isThirdImplementationAllowed(Third third) {
            return identity.hasRole("admin") && third.value().equals("class");
        }

        @PermissionChecker("thriceThird")
        boolean areAllThreeOk(Third theAbstract, Third theInterface, Third implementation) {
            return theAbstract.value.equals("abstract") && theInterface.value.equals("interface")
                    && implementation.value.equals("implementation");
        }

        @PermissionChecker("generic-checker")
        <T extends Top> boolean genericChecker(T top) {
            return top.value().equals("generic");
        }
    }
}
