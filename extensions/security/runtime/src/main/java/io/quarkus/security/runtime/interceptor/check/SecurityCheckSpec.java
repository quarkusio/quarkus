package io.quarkus.security.runtime.interceptor.check;

import java.util.List;

public sealed interface SecurityCheckSpec
        permits SecurityCheckSpec.Standard, SecurityCheckSpec.RolesAllowed, SecurityCheckSpec.PermissionsAllowed {

    enum Standard implements SecurityCheckSpec {
        PERMIT_ALL,
        DENY_ALL,
        AUTHENTICATED
    }

    record RolesAllowed(List<String> roles, List<Integer> configExpressionIndexes,
            List<Integer> configKeys) implements SecurityCheckSpec {

        public RolesAllowed {
            roles = List.copyOf(roles);
            configExpressionIndexes = List.copyOf(configExpressionIndexes);
            configKeys = List.copyOf(configKeys);
            if (configExpressionIndexes.size() != configKeys.size()) {
                throw new IllegalArgumentException("Each configuration expression index requires exactly one key");
            }
        }

        public static RolesAllowed of(List<String> roles) {
            return new RolesAllowed(roles, List.of(), List.of());
        }

        public boolean hasConfigExpressions() {
            return !configExpressionIndexes.isEmpty();
        }
    }

    record PermissionsAllowed(List<List<PermissionSpec>> permissionGroups, boolean computed) implements SecurityCheckSpec {

        public PermissionsAllowed {
            permissionGroups = permissionGroups.stream().map(List::copyOf).toList();
            if (permissionGroups.isEmpty()) {
                throw new IllegalArgumentException("At least one permission group is required");
            }
        }
    }

    record PermissionSpec(String name, String permissionClass, List<String> actions, boolean passActionsToConstructor,
            boolean computed, List<Integer> formalParamIndexes, List<String> formalParamConverters) {

        public PermissionSpec {
            actions = actions == null ? null : List.copyOf(actions);
            formalParamIndexes = List.copyOf(formalParamIndexes);
            formalParamConverters = List.copyOf(formalParamConverters);
            if (computed && formalParamIndexes.size() != formalParamConverters.size()) {
                throw new IllegalArgumentException("Each formal parameter index requires exactly one converter entry");
            }
        }

        public static PermissionSpec plain(String name, String permissionClass, List<String> actions,
                boolean passActionsToConstructor) {
            return new PermissionSpec(name, permissionClass, actions, passActionsToConstructor, false, List.of(), List.of());
        }
    }
}
