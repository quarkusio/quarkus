package io.quarkus.resteasy.test.security;

import java.security.Permission;
import java.util.Objects;

public class CustomPermissionWithExtraArgs extends Permission {

    private final String permName;
    private final String goodbye;
    private final String toWhom;
    private final int day;
    private final String place;

    public CustomPermissionWithExtraArgs(String permName, String goodbye, String toWhom, int day, String place) {
        super(permName);
        this.permName = permName;
        this.goodbye = goodbye;
        this.toWhom = toWhom;
        this.day = day;
        this.place = place;
    }

    @Override
    public boolean implies(Permission permission) {
        if (permission instanceof CustomPermissionWithExtraArgs) {
            return permission.equals(this);
        }
        return false;
    }

    @Override
    public String getActions() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CustomPermissionWithExtraArgs that = (CustomPermissionWithExtraArgs) o;
        return day == that.day && Objects.equals(permName, that.permName) && Objects.equals(goodbye, that.goodbye)
                && Objects.equals(toWhom, that.toWhom) && Objects.equals(place, that.place);
    }

    @Override
    public int hashCode() {
        return Objects.hash(permName, goodbye, toWhom, day, place);
    }
}
