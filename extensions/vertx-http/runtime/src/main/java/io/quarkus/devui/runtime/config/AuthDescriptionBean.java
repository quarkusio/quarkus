package io.quarkus.devui.runtime.config;

import java.util.List;

public class AuthDescriptionBean {

    private final List<PermissionSet> permissionGroups;
    private final List<PermissionSet> policyGroups;

    public AuthDescriptionBean(final List<PermissionSet> permissionGroups, final List<PermissionSet> policyGroups) {
        this.permissionGroups = permissionGroups;
        this.policyGroups = policyGroups;
    }

    public List<PermissionSet> getAllPermissionGroups() {
        return permissionGroups;
    }

    public List<PermissionSet> getAllPolicyGroups() {
        return policyGroups;
    }
}
