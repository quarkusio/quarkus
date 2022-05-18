package io.quarkus.it.keycloak;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.jboss.logging.Logger;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.util.JsonSerialization;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

public class KeycloakLifecycleManager implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = Logger.getLogger(KeycloakLifecycleManager.class);
    private GenericContainer<?> keycloak;

    protected static String KEYCLOAK_SERVER_URL;
    private static final String KEYCLOAK_REALM = "quarkus";
    private static final String KEYCLOAK_SERVICE_CLIENT = "quarkus-service-app";
    private static final String KEYCLOAK_VERSION = System.getProperty("keycloak.version");

    @SuppressWarnings("resource")
    @Override
    public Map<String, String> start() {
        keycloak = new GenericContainer<>("quay.io/keycloak/keycloak:" + KEYCLOAK_VERSION)
                .withExposedPorts(8080)
                .withEnv("KEYCLOAK_ADMIN", "admin")
                .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin")
                .waitingFor(Wait.forLogMessage(".*Keycloak.*started.*", 1));

        keycloak = keycloak
                .withCopyToContainer(Transferable.of(createPoliciesJar().toByteArray()), "/opt/keycloak/providers/policies.jar")
                .withCommand("start-dev");
        keycloak.start();
        LOGGER.info(keycloak.getLogs());

        KEYCLOAK_SERVER_URL = "http://localhost:" + keycloak.getMappedPort(8080);

        RealmRepresentation realm = createRealm(KEYCLOAK_REALM);

        postRealm(realm);

        Map<String, String> properties = new HashMap<>();

        properties.put("quarkus.oidc.auth-server-url", KEYCLOAK_SERVER_URL + "/realms/" + KEYCLOAK_REALM);
        properties.put("keycloak.url", KEYCLOAK_SERVER_URL);

        return properties;
    }

    private ByteArrayOutputStream createPoliciesJar() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (
                ZipOutputStream zos = new ZipOutputStream(out);) {

            List<URL> entryUrls = List.of(
                    getClass().getResource("/policies/admin-policy.js"),
                    getClass().getResource("/policies/always-grant.js"),
                    getClass().getResource("/policies/body-claim-based-policy.js"),
                    getClass().getResource("/policies/claim-based-policy.js"),
                    getClass().getResource("/policies/confidential-policy.js"),
                    getClass().getResource("/policies/http-claim-based-policy.js"),
                    getClass().getResource("/policies/superuser-policy.js"));

            addZipEntry("META-INF/keycloak-scripts.json", getClass().getResource("/policies/META-INF/keycloak-scripts.json"),
                    zos);

            for (URL entryUrl : entryUrls) {
                File entryFile = Paths.get(entryUrl.toURI()).toFile();
                addZipEntry(entryFile.getName(), entryUrl, zos);
            }
        } catch (Exception cause) {
            throw new RuntimeException("Failed to create policies file", cause);
        }

        return out;
    }

    private void addZipEntry(String entryName, URL entryUrl, ZipOutputStream zos) throws URISyntaxException, IOException {
        ZipEntry zipEntry = new ZipEntry(entryName);

        zos.putNextEntry(zipEntry);
        zos.write(Files.readAllBytes(Paths.get(entryUrl.toURI())));
        zos.closeEntry();
    }

    private static void postRealm(RealmRepresentation realm) {
        try {
            createRequestSpec().auth().oauth2(getAdminAccessToken())
                    .contentType("application/json")
                    .body(JsonSerialization.writeValueAsBytes(realm))
                    .when()
                    .post(KEYCLOAK_SERVER_URL + "/admin/realms").then()
                    .statusCode(201);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    private static String getAdminAccessToken() {
        return createRequestSpec()
                .param("grant_type", "password")
                .param("username", "admin")
                .param("password", "admin")
                .param("client_id", "admin-cli")
                .when()
                .post(KEYCLOAK_SERVER_URL + "/realms/master/protocol/openid-connect/token")
                .as(AccessTokenResponse.class).getToken();
    }

    private static ClientRepresentation createServiceClient(String clientId) {
        ClientRepresentation client = new ClientRepresentation();

        client.setClientId(clientId);
        client.setPublicClient(false);
        client.setSecret("secret");
        client.setDirectAccessGrantsEnabled(true);
        client.setServiceAccountsEnabled(true);
        client.setEnabled(true);

        return client;
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

    public static String getAccessToken(String userName) {
        return createRequestSpec().param("grant_type", "password")
                .param("username", userName)
                .param("password", userName)
                .param("client_id", KEYCLOAK_SERVICE_CLIENT)
                .param("client_secret", "secret")
                .when()
                .post(KEYCLOAK_SERVER_URL + "/realms/" + KEYCLOAK_REALM + "/protocol/openid-connect/token")
                .as(AccessTokenResponse.class).getToken();
    }

    public static String getRefreshToken(String userName) {
        return createRequestSpec().param("grant_type", "password")
                .param("username", userName)
                .param("password", userName)
                .param("client_id", KEYCLOAK_SERVICE_CLIENT)
                .param("client_secret", "secret")
                .when()
                .post(KEYCLOAK_SERVER_URL + "/realms/" + KEYCLOAK_REALM + "/protocol/openid-connect/token")
                .as(AccessTokenResponse.class).getRefreshToken();
    }

    @Override
    public void stop() {
        createRequestSpec().auth().oauth2(getAdminAccessToken())
                .when()
                .delete(KEYCLOAK_SERVER_URL + "/admin/realms/" + KEYCLOAK_REALM).then().statusCode(204);

        keycloak.stop();
    }

    private static RequestSpecification createRequestSpec() {
        return RestAssured.given().relaxedHTTPSValidation();
    }
}
