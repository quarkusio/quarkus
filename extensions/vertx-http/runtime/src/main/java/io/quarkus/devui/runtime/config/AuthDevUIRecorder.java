package io.quarkus.devui.runtime.config;

import java.util.*;
import java.util.function.Supplier;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class AuthDevUIRecorder {

    private static List<PermissionSet> permissionGroups;
    private static List<PermissionSet> policyGroups;

    public void registerPermissionGroups(final List<PermissionSet> permissionGroups,
            final List<PermissionSet> policyGroups) {
        AuthDevUIRecorder.permissionGroups = permissionGroups;
        AuthDevUIRecorder.policyGroups = policyGroups;
    }

    public Supplier<AuthDescriptionBean> permissionGroupBean() {
        return () -> new AuthDescriptionBean(permissionGroups, policyGroups);
    }

}
