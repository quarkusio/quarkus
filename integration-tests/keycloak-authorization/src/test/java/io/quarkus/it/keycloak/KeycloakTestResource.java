package io.quarkus.it.keycloak;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class KeycloakTestResource implements QuarkusTestResourceLifecycleManager {

    private static final String KEYCLOAK_SERVER_URL = System.getProperty("keycloak.url", "http://localhost:8180/auth");
    private static final String KEYCLOAK_REALM = "quarkus";

    private Keycloak keycloak;

    @Override
    public Map<String, String> start() {

        RealmRepresentation realm = createRealm(KEYCLOAK_REALM);

        realm.getClients().add(createClient("quarkus-app"));
        realm.getUsers().add(createUser("alice", "user", "superuser"));
        realm.getUsers().add(createUser("admin", "user", "admin"));
        realm.getUsers().add(createUser("jdoe", "user", "confidential"));

        keycloak = KeycloakBuilder.builder()
                .serverUrl(KEYCLOAK_SERVER_URL)
                .realm("master")
                .clientId("admin-cli")
                .username("admin")
                .password("admin")
                .build();
        keycloak.realms().create(realm);

        return Collections.emptyMap();
    }

    private static RealmRepresentation createRealm(String name) {
        RealmRepresentation realm = new RealmRepresentation();

        realm.setRealm(name);
        realm.setEnabled(true);
        realm.setUsers(new ArrayList<>());
        realm.setClients(new ArrayList<>());

        RolesRepresentation roles = new RolesRepresentation();
        List<RoleRepresentation> realmRoles = new ArrayList<>();

        roles.setRealm(realmRoles);
        realm.setRoles(roles);

        realm.getRoles().getRealm().add(new RoleRepresentation("user", null, false));
        realm.getRoles().getRealm().add(new RoleRepresentation("superuser", null, false));
        realm.getRoles().getRealm().add(new RoleRepresentation("admin", null, false));
        realm.getRoles().getRealm().add(new RoleRepresentation("confidential", null, false));

        return realm;
    }

    private static ClientRepresentation createClient(String clientId) {
        ClientRepresentation client = new ClientRepresentation();

        client.setClientId(clientId);
        client.setRedirectUris(Arrays.asList("*"));
        client.setPublicClient(false);
        client.setSecret("secret");
        client.setDirectAccessGrantsEnabled(true);
        client.setEnabled(true);

        client.setAuthorizationServicesEnabled(true);

        ResourceServerRepresentation authorizationSettings = new ResourceServerRepresentation();

        authorizationSettings.setResources(new ArrayList<>());
        authorizationSettings.setPolicies(new ArrayList<>());

        configurePermissionResourcePermission(authorizationSettings);
        configureClaimBasedPermission(authorizationSettings);
        configureHttpResponseClaimBasedPermission(authorizationSettings);
        configureBodyClaimBasedPermission(authorizationSettings);
        configurePaths(authorizationSettings);
        configureScopePermission(authorizationSettings);

        client.setAuthorizationSettings(authorizationSettings);

        return client;
    }

    private static void configurePermissionResourcePermission(ResourceServerRepresentation settings) {
        PolicyRepresentation policyConfidential = createJSPolicy("Confidential Policy",
                "var identity = $evaluation.context.identity;\n" +
                        "\n" +
                        "if (identity.hasRealmRole(\"confidential\")) {\n" +
                        "$evaluation.grant();\n" +
                        "}",
                settings);
        createPermission(settings, createResource(settings, "Permission Resource", "/api/permission"), policyConfidential);

        PolicyRepresentation policyAdmin = createJSPolicy("Admin Policy", "var identity = $evaluation.context.identity;\n" +
                "\n" +
                "if (identity.hasRealmRole(\"admin\")) {\n" +
                "$evaluation.grant();\n" +
                "}", settings);

        createPermission(settings, createResource(settings, "Permission Resource Tenant", "/api-permission-tenant"),
                policyAdmin);

        PolicyRepresentation policyUser = createJSPolicy("Superuser Policy", "var identity = $evaluation.context.identity;\n" +
                "\n" +
                "if (identity.hasRealmRole(\"superuser\")) {\n" +
                "$evaluation.grant();\n" +
                "}", settings);

        createPermission(settings, createResource(settings, "Permission Resource WebApp", "/api-permission-webapp"),
                policyUser);
    }

    private static void configureScopePermission(ResourceServerRepresentation settings) {
        PolicyRepresentation policy = createJSPolicy("Grant Policy", "$evaluation.grant();", settings);
        createScopePermission(settings,
                createResource(settings, "Scope Permission Resource", "/api/permission/scope", "read", "write"), policy,
                "read");
    }

    private static void configureClaimBasedPermission(ResourceServerRepresentation settings) {
        PolicyRepresentation policy = createJSPolicy("Claim-Based Policy", "var context = $evaluation.getContext();\n"
                + "var attributes = context.getAttributes();\n"
                + "\n"
                + "if (attributes.containsValue('grant', 'true')) {\n"
                + "    $evaluation.grant();\n"
                + "}", settings);
        createPermission(settings, createResource(settings, "Claim Protected Resource", "/api/permission/claim-protected"),
                policy);
    }

    private static void configureHttpResponseClaimBasedPermission(ResourceServerRepresentation settings) {
        PolicyRepresentation policy = createJSPolicy("Http Response Claim-Based Policy",
                "var context = $evaluation.getContext();\n"
                        + "var attributes = context.getAttributes();\n"
                        + "\n"
                        + "if (attributes.containsValue('user-name', 'alice')) {\n"
                        + "    $evaluation.grant();\n"
                        + "}",
                settings);
        createPermission(settings, createResource(settings, "Http Response Claim Protected Resource",
                "/api/permission/http-response-claim-protected"), policy);
    }

    private static void configureBodyClaimBasedPermission(ResourceServerRepresentation settings) {
        PolicyRepresentation policy = createJSPolicy("Body Claim-Based Policy",
                "var context = $evaluation.getContext();\n"
                        + "print(context.getAttributes().toMap());"
                        + "var attributes = context.getAttributes();\n"
                        + "\n"
                        + "if (attributes.containsValue('from-body', 'grant')) {\n"
                        + "    $evaluation.grant();\n"
                        + "}",
                settings);
        createPermission(settings, createResource(settings, "Body Claim Protected Resource",
                "/api/permission/body-claim"), policy);
    }

    private static void configurePaths(ResourceServerRepresentation settings) {
        createResource(settings, "Root", null);
        createResource(settings, "API", "/api2/*");
        createResource(settings, "Hello", "/hello");
    }

    private static void createPermission(ResourceServerRepresentation settings, ResourceRepresentation resource,
            PolicyRepresentation policy) {
        PolicyRepresentation permission = new PolicyRepresentation();

        permission.setName(resource.getName() + " Permission");
        permission.setType("resource");
        permission.setResources(new HashSet<>());
        permission.getResources().add(resource.getName());
        permission.setPolicies(new HashSet<>());
        permission.getPolicies().add(policy.getName());

        settings.getPolicies().add(permission);
    }

    private static void createScopePermission(ResourceServerRepresentation settings, ResourceRepresentation resource,
            PolicyRepresentation policy, String scope) {
        PolicyRepresentation permission = new PolicyRepresentation();

        permission.setName(resource.getName() + " Permission");
        permission.setType("scope");
        permission.setResources(new HashSet<>());
        permission.getResources().add(resource.getName());
        permission.setScopes(new HashSet<>());
        permission.getScopes().add(scope);
        permission.setPolicies(new HashSet<>());
        permission.getPolicies().add(policy.getName());

        settings.getPolicies().add(permission);
    }

    private static ResourceRepresentation createResource(ResourceServerRepresentation authorizationSettings, String name,
            String uri, String... scopes) {
        ResourceRepresentation resource = new ResourceRepresentation(name);

        for (String scope : scopes) {
            resource.addScope(scope);
        }

        if (uri != null) {
            resource.setUris(Collections.singleton(uri));
        }

        authorizationSettings.getResources().add(resource);
        return resource;
    }

    private static PolicyRepresentation createJSPolicy(String name, String code, ResourceServerRepresentation settings) {
        PolicyRepresentation policy = new PolicyRepresentation();

        policy.setName(name);
        policy.setType("js");
        policy.setConfig(new HashMap<>());
        policy.getConfig().put("code", code);

        settings.getPolicies().add(policy);

        return policy;
    }

    private static UserRepresentation createUser(String username, String... realmRoles) {
        UserRepresentation user = new UserRepresentation();

        user.setUsername(username);
        user.setEnabled(true);
        user.setCredentials(new ArrayList<>());
        user.setRealmRoles(Arrays.asList(realmRoles));

        CredentialRepresentation credential = new CredentialRepresentation();

        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(username);
        credential.setTemporary(false);

        user.getCredentials().add(credential);

        return user;
    }

    @Override
    public void stop() {
        keycloak.realm(KEYCLOAK_REALM).remove();

    }
}
