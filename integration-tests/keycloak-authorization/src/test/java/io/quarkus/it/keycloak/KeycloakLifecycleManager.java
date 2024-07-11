package io.quarkus.it.keycloak;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.keycloak.client.KeycloakTestClient;

public class KeycloakLifecycleManager implements QuarkusTestResourceLifecycleManager, DevServicesContext.ContextAware {
    private static final String KEYCLOAK_REALM = "quarkus";
    final KeycloakTestClient client = new KeycloakTestClient();

    @Override
    public Map<String, String> start() {
        RealmRepresentation realm = createRealm(KEYCLOAK_REALM);

        client.createRealm(realm);
        return Map.of();
    }

    private static RealmRepresentation createRealm(String name) {
        RealmRepresentation realm = new RealmRepresentation();

        realm.setRealm(name);
        realm.setEnabled(true);
        realm.setUsers(new ArrayList<>());
        realm.setClients(new ArrayList<>());
        realm.setAccessTokenLifespan(3);
        realm.setSsoSessionMaxLifespan(3);

        RolesRepresentation roles = new RolesRepresentation();
        List<RoleRepresentation> realmRoles = new ArrayList<>();

        roles.setRealm(realmRoles);
        realm.setRoles(roles);

        realm.getRoles().getRealm().add(new RoleRepresentation("user", null, false));
        realm.getRoles().getRealm().add(new RoleRepresentation("superuser", null, false));
        realm.getRoles().getRealm().add(new RoleRepresentation("admin", null, false));
        realm.getRoles().getRealm().add(new RoleRepresentation("confidential", null, false));

        realm.getClients().add(createClient("quarkus-app"));
        realm.getUsers().add(createUser("alice", "user", "superuser"));
        realm.getUsers().add(createUser("admin", "user", "admin"));
        realm.getUsers().add(createUser("jdoe", "user", "confidential"));

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
                "confidential-policy.js",
                settings);
        createPermission(settings, createResource(settings, "Permission Resource", "/api/permission"), policyConfidential);

        PolicyRepresentation policyAdmin = createJSPolicy("Admin Policy", "admin-policy.js", settings);

        createPermission(settings, createResource(settings, "Permission Resource Tenant", "/api-permission-tenant"),
                policyAdmin);

        createPermission(settings,
                createResource(settings, "Dynamic Config Permission Resource Tenant", "/dynamic-permission-tenant"),
                policyAdmin);

        PolicyRepresentation policyUser = createJSPolicy("Superuser Policy", "superuser-policy.js", settings);

        createPermission(settings, createResource(settings, "Permission Resource WebApp", "/api-permission-webapp"),
                policyUser);
    }

    private static void configureScopePermission(ResourceServerRepresentation settings) {
        PolicyRepresentation policy = createJSPolicy("Grant Policy", "always-grant.js", settings);
        createScopePermission(settings,
                createResource(settings, "Scope Permission Resource", "/api/permission/scope", "read", "write"), policy,
                "read");
    }

    private static void configureClaimBasedPermission(ResourceServerRepresentation settings) {
        PolicyRepresentation policy = createJSPolicy("Claim-Based Policy", "claim-based-policy.js", settings);
        createPermission(settings, createResource(settings, "Claim Protected Resource", "/api/permission/claim-protected"),
                policy);
    }

    private static void configureHttpResponseClaimBasedPermission(ResourceServerRepresentation settings) {
        PolicyRepresentation policy = createJSPolicy("Http Response Claim-Based Policy",
                "http-claim-based-policy.js",
                settings);
        createPermission(settings, createResource(settings, "Http Response Claim Protected Resource",
                "/api/permission/http-response-claim-protected"), policy);
    }

    private static void configureBodyClaimBasedPermission(ResourceServerRepresentation settings) {
        PolicyRepresentation policy = createJSPolicy("Body Claim-Based Policy",
                "body-claim-based-policy.js",
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
        policy.setType("script-" + code);

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
        //client.deleteRealm(KEYCLOAK_REALM);
    }

    @Override
    public void setIntegrationTestContext(DevServicesContext context) {
        client.setIntegrationTestContext(context);
    }
}
