package io.quarkus.devui.runtime.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PermissionSet implements Comparable<PermissionSet> {

    public final static String QUARKUS_NULL_VALUE = "QuarkusNullValue"; // vaadin-list-box component doesn't handle null values

    private String name;
    private boolean isEnabled;
    private String authMechanism = QUARKUS_NULL_VALUE;
    private String policy = QUARKUS_NULL_VALUE;
    private List<String> methods = new ArrayList<>();
    private List<String> paths = new ArrayList<>();
    private List<PermissionDescription> permissionDescriptions;

    public PermissionSet() {
    }

    public PermissionSet(List<PermissionDescription> permissionDescriptions) {
        this.permissionDescriptions = permissionDescriptions;
        this.name = permissionDescriptions.get(0).getConfigId();

        Optional<PermissionDescription> optionalPolicy = permissionDescriptions.stream()
                .filter(p -> p.getType() == AuthFieldType.PERMISSION_POLICY)
                .findFirst();

        this.policy = optionalPolicy.map(PermissionDescription::getValue).orElse(QUARKUS_NULL_VALUE);

        Optional<PermissionDescription> optionalAuthMechanism = permissionDescriptions.stream()
                .filter(p -> p.getType() == AuthFieldType.PERMISSION_AUTH_MECHANISM)
                .findFirst();

        this.authMechanism = optionalAuthMechanism.map(PermissionDescription::getValue).orElse(QUARKUS_NULL_VALUE);

        this.methods = permissionDescriptions
                .stream()
                .filter(p -> p.getType() == AuthFieldType.PERMISSION_METHODS)
                .map(PermissionDescription::getValue)
                .map(s -> Arrays.asList(s.split(",")))
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());

        this.paths = permissionDescriptions
                .stream()
                .filter(p -> p.getType() == AuthFieldType.PERMISSION_PATHS)
                .map(PermissionDescription::getValue)
                .map(s -> Arrays.asList(s.split(",")))
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());

        Optional<PermissionDescription> optionalEnabled = permissionDescriptions.stream()
                .filter(p -> p.getType() == AuthFieldType.PERMISSION_ENABLED)
                .findFirst();
        // if it's not defined, we consider that is enabled, otherwise set the config value
        this.isEnabled = optionalEnabled.map(permissionDescription -> Boolean.parseBoolean(permissionDescription.getValue()))
                .orElse(true);
    }

    public List<String> getMethods() {
        return methods;
    }

    public void setMethods(List<String> methods) {
        this.methods = methods;
    }

    public String getAuthMechanism() {
        return authMechanism;
    }

    public void setAuthMechanism(String authMechanism) {
        this.authMechanism = authMechanism;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<PermissionDescription> getPermissionDescriptions() {
        return permissionDescriptions;
    }

    public void setPermissionDescriptions(List<PermissionDescription> permissionDescriptions) {
        this.permissionDescriptions = permissionDescriptions;
    }

    @Override
    public int compareTo(PermissionSet permissionSet) {
        return this.name.compareTo(permissionSet.name);
    }

}
