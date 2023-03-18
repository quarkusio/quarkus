package io.quarkus.elytron.security.jdbc.it;

import java.security.Permission;
import java.util.Arrays;
import java.util.Objects;

import io.quarkus.arc.Arc;
import io.quarkus.security.PermissionsAllowed;

/**
 * Permit access if secured method (one annotated with {@link PermissionsAllowed} using this permission has parameter
 * 'day' with actual value 'Monday', 'Tuesday', 'Wednesday', 'Thursday' or 'Friday'. Secondary check is based on user
 * actions.
 */
public class WorkdayPermission extends Permission {

    private final String[] actions;
    private final String day;

    /**
     * Constructs a permission with the specified name, actions and String parameter 'day'.
     * Every method secured with {@link io.quarkus.security.PermissionsAllowed} whose {@link PermissionsAllowed#permission()}
     * matches this class must have a formal parameter {@link String} named 'day'.
     *
     * @param name name of the Permission object being created.
     * @param actions Permission actions
     * @param day workday
     */
    public WorkdayPermission(String name, String[] actions, String day) {
        super(name);
        this.actions = actions;
        this.day = day;
    }

    @Override
    public boolean implies(Permission permission) {
        if (permission instanceof WorkdayPermission) {

            WorkdayPermission that = (WorkdayPermission) permission;
            // verify Permission name and actions has been passed to the constructor
            if (that.getName().equals("worker") && that.getActions().contains("adult")) {

                // verify we can obtain bean instance
                final WorkdayEvaluator workdayEvaluator = Arc.container().instance(WorkdayEvaluator.class).get();
                return workdayEvaluator.isWorkday(that.day);
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        WorkdayPermission that = (WorkdayPermission) o;
        return Arrays.equals(actions, that.actions) && Objects.equals(day, that.day);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(day);
        result = 31 * result + Arrays.hashCode(actions);
        return result;
    }

    @Override
    public String getActions() {
        return String.join(",", actions);
    }
}
