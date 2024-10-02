package io.quarkus.security.test.permissionsallowed;

import java.security.BasicPermission;
import java.security.Permission;

public class CustomPermissionWithStringArg extends BasicPermission {

    public static final String EXPECTED_FIELD_STRING_ARGUMENT = "expectedFieldStringArgument";

    private final String arg;

    public CustomPermissionWithStringArg(String name, String propertyOne) {
        super(name);
        this.arg = propertyOne;
    }

    @Override
    public boolean implies(Permission p) {
        return EXPECTED_FIELD_STRING_ARGUMENT.equals(arg);
    }
}
