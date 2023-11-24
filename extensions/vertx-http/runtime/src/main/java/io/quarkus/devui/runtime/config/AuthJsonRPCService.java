package io.quarkus.devui.runtime.config;

import static io.quarkus.devui.runtime.config.PermissionSet.QUARKUS_NULL_VALUE;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devui.runtime.comms.JsonRpcMessage;
import io.quarkus.devui.runtime.comms.MessageType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class AuthJsonRPCService {
    private static final Logger LOG = Logger.getLogger(AuthJsonRPCService.class.getName());
    private final List<String> METHODS = List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD", "CONNECT",
            "TRACE"); // do not change order
    private final List<String> AUTH_MECHANISM = List.of(QUARKUS_NULL_VALUE, "basic", "bearer", "form"); // do not change order
    private final List<String> POLICIES = List.of("permit", "deny", "authenticated"); // do not change order

    @Inject
    AuthDescriptionBean authDescriptionBean;

    static class PermissionSetUpdateRequest {
        public String id;
        public Map<String, Object> values;
    }

    public JsonRpcMessage<Void> deletePermissionSet(String name) {
        Map<String, String> map = new HashMap<>();

        map.put(AuthFieldType.PERMISSION_ENABLED.convertToKey(name), "");
        map.put(AuthFieldType.PERMISSION_POLICY.convertToKey(name), "");
        map.put(AuthFieldType.PERMISSION_METHODS.convertToKey(name), "");
        map.put(AuthFieldType.PERMISSION_PATHS.convertToKey(name), "");
        map.put(AuthFieldType.PERMISSION_AUTH_MECHANISM.convertToKey(name), "");
        map.put(AuthFieldType.POLICY_ROLES_ALLOWED.convertToKey(name), "");
        map.put(AuthFieldType.POLICY_PERMISSIONS.convertToKey(name), "");
        map.put(AuthFieldType.POLICY_PERMISSION_CLASS.convertToKey(name), "");

        DevConsoleManager.invoke("update-permission-set", map);
        return new JsonRpcMessage(true, MessageType.Void);
    }

    public JsonObject updatePermissionSet(PermissionSetUpdateRequest permissionValues) {

        Map<String, String> map = new HashMap<>();

        for (Map.Entry<String, Object> entry : permissionValues.values.entrySet()) {
            AuthFieldType authFieldType = AuthFieldType.valueOf(entry.getKey());
            map.put(authFieldType.convertToKey(permissionValues.id),
                    authFieldType.convertToValue(permissionValues.id, entry.getValue()));
        }

        DevConsoleManager.invoke("update-permission-set", map);

        return this.getAllPermissionGroupsConfig();
    }

    public JsonObject getAllPermissionGroupsConfig() {

        List<PermissionSet> allPermissionGroups = authDescriptionBean.getAllPermissionGroups();
        JsonObject metadata = new JsonObject();

        List<String> finalMethods = new ArrayList<>(METHODS);

        allPermissionGroups.stream()
                .map(PermissionSet::getMethods)
                .flatMap(List::stream)
                .sorted()
                .filter(Predicate.not(finalMethods::contains))
                .forEachOrdered(finalMethods::add);

        List<String> finalPaths = allPermissionGroups.stream()
                .map(PermissionSet::getPaths)
                .flatMap(List::stream)
                .sorted()
                .distinct()
                .collect(Collectors.toList());

        List<String> finalAuthMechanisms = new ArrayList<>(AUTH_MECHANISM);

        allPermissionGroups.stream()
                .map(PermissionSet::getAuthMechanism)
                .sorted()
                .filter(Predicate.not(finalAuthMechanisms::contains))
                .forEachOrdered(finalAuthMechanisms::add);

        List<String> finalPolicies = new ArrayList<>(POLICIES);

        allPermissionGroups.stream()
                .map(PermissionSet::getPolicy)
                .sorted()
                .filter(Predicate.not(finalPolicies::contains))
                .forEachOrdered(finalPolicies::add);

        metadata.put("methods", finalMethods);
        metadata.put("paths", finalPaths);
        metadata.put("authMechanisms", finalAuthMechanisms);
        metadata.put("policies", finalPolicies);

        JsonObject entries = new JsonObject();
        entries.put("metadata", metadata);
        entries.put("permissions", allPermissionGroups);

        return entries;
    }

    public JsonArray getAllPermissionGroups() {
        return new JsonArray(authDescriptionBean.getAllPermissionGroups());
    }

    public JsonArray getAllPolicyGroups() {
        return new JsonArray(authDescriptionBean.getAllPolicyGroups());
    }

}
