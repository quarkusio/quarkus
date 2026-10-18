package io.quarkus.security.test.permissionsallowed.checker;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.security.PermissionChecker;
import io.quarkus.security.identity.SecurityIdentity;

@ApplicationScoped
class StringArrayPermissionCheckers {

    static final String EXPECTED_ARRAY_LENGTH = "expected-array-length";

    @Inject
    SecurityIdentity injectedIdentity;

    @PermissionChecker("string-varargs")
    boolean stringVarargsArgument(String... arguments) {
        return authorize(injectedIdentity, arguments);
    }

    @PermissionChecker("security-identity-and-string-varargs")
    boolean securityIdentityAndStringVarargsArgument(SecurityIdentity securityIdentity, String... arguments) {
        return authorize(securityIdentity, arguments);
    }

    @PermissionChecker("single-string-array")
    boolean singleStringArrayArgument(String[] arguments) {
        return authorize(injectedIdentity, arguments);
    }

    @PermissionChecker("two-string-array")
    boolean twoStringArrayArguments(String[] arguments1, String[] arguments2) {
        return authorize(injectedIdentity, arguments1, arguments2);
    }

    @PermissionChecker("single-string-array-and-security-identity")
    boolean stringArrayArgumentAndSecurityIdentity(String[] arguments, SecurityIdentity securityIdentity) {
        return authorize(securityIdentity, arguments);
    }

    @PermissionChecker("two-string-arrays-and-security-identity")
    boolean twoStringArrayArgumentsAndSecurityIdentity(String[] arguments1, SecurityIdentity securityIdentity,
            String[] arguments2) {
        return authorize(securityIdentity, arguments1, arguments2);
    }

    @PermissionChecker("security-identity-and-single-string-array")
    boolean securityIdentityAndStringArrayArgument(SecurityIdentity securityIdentity, String[] arguments) {
        return authorize(securityIdentity, arguments);
    }

    @PermissionChecker("string-and-string-array-arguments-static-method")
    boolean stringAndStringArrayArgumentsStaticMethod(String string, String[] arguments) {
        if (!"expected".equals(string)) {
            logDebugMessage("String argument value should be 'expected', but found " + string);
            return false;
        }
        return authorize(injectedIdentity, arguments);
    }

    @PermissionChecker("string-varargs-static-method")
    boolean stringVarargsStaticMethod(String... arguments) {
        return authorize(injectedIdentity, arguments);
    }

    @PermissionChecker("security-identity-string-varargs-static-method")
    boolean securityIdentityStringVarargsStaticMethod(SecurityIdentity securityIdentity, String... arguments) {
        return authorize(securityIdentity, arguments);
    }

    @PermissionChecker("string-array-and-multiple-arguments")
    boolean stringArrayAndMultipleArguments(String[] anotherArguments, Long id, int pk, String[] arguments) {
        if (id != 5) {
            logDebugMessage("Long argument should be 5, but was " + id);
            return false;
        }
        if (pk != 3) {
            logDebugMessage("int argument should be 3, but was " + pk);
            return false;
        }
        if (!authorize(injectedIdentity, anotherArguments, arguments)) {
            return false;
        }
        if (!"anotherArguments".equals(anotherArguments[0])) {
            return false;
        }
        if (!"arguments".equals(arguments[0])) {
            return false;
        }
        return true;
    }

    private static boolean authorize(SecurityIdentity securityIdentity, String[]... args) {
        for (String[] arg : args) {
            if (arg == null) {
                logDebugMessage("Permission checker argument is null");
                return false;
            }
            int actualArrayLength = arg.length;
            int expectedArrayLength = securityIdentity.getAttribute(EXPECTED_ARRAY_LENGTH);
            if (actualArrayLength != expectedArrayLength) {
                logDebugMessage("Expected array length " + expectedArrayLength + " but found " + actualArrayLength);
                return false;
            }
        }
        return true;
    }

    private static void logDebugMessage(String message) {
        var logger = Logger.getLogger(StringArrayPermissionCheckers.class.getName());
        logger.debug(message);
    }
}
