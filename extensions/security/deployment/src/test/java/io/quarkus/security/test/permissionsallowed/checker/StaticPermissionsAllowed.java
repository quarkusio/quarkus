package io.quarkus.security.test.permissionsallowed.checker;

import io.quarkus.security.PermissionsAllowed;

class StaticPermissionsAllowed {

    StaticPermissionsAllowed() {
    }

    @PermissionsAllowed("string-and-string-array-arguments-static-method")
    static String stringAndStringArrayArgumentsStaticMethod(String string, String[] arguments) {
        return "stringAndStringArrayArgumentsStaticMethod";
    }

    @PermissionsAllowed("string-varargs-static-method")
    static String stringVarargsStaticMethod(String... arguments) {
        return "stringVarargsStaticMethod";
    }

    @PermissionsAllowed("security-identity-string-varargs-static-method")
    static String securityIdentityStringVarargsStaticMethod(String... arguments) {
        return "securityIdentityStringVarargsStaticMethod";
    }

}
