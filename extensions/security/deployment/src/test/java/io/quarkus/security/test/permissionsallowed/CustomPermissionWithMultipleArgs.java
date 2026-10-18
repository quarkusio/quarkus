package io.quarkus.security.test.permissionsallowed;

import java.security.BasicPermission;
import java.security.Permission;

public class CustomPermissionWithMultipleArgs extends BasicPermission {

    public static final String EXPECTED_FIELD_STRING_ARGUMENT = "expectedFieldStringArgument";
    public static final int EXPECTED_FIELD_INT_ARGUMENT = 100;
    public static final long EXPECTED_FIELD_LONG_ARGUMENT = 357;

    private final String arg;
    private final int fourth;
    private final long first;

    public CustomPermissionWithMultipleArgs(String name, String propertyOne, int fourth, long first) {
        super(name);
        this.arg = propertyOne;
        this.first = first;
        this.fourth = fourth;
    }

    @Override
    public boolean implies(Permission p) {
        return EXPECTED_FIELD_STRING_ARGUMENT.equals(arg) && EXPECTED_FIELD_INT_ARGUMENT == fourth
                && EXPECTED_FIELD_LONG_ARGUMENT == first;
    }
}
