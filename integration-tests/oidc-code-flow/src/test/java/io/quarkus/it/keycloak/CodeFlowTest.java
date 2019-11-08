package io.quarkus.it.keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@QuarkusTest
public class CodeFlowTest {

    private static final String KEYCLOAK_SERVER_URL = System.getProperty("keycloak.url", "http://localhost:8180/auth");
    private static final String KEYCLOAK_REALM = "quarkus";

    @BeforeAll
    public static void configureKeycloakRealm() throws IOException {
        RealmRepresentation realm = createRealm(KEYCLOAK_REALM);

        realm.getClients().add(createClient("quarkus-app"));
        realm.getUsers().add(createUser("alice", "user"));
        realm.getUsers().add(createUser("admin", "user", "admin"));
        realm.getUsers().add(createUser("jdoe", "user", "confidential"));

        RestAssured
                .given()
                .auth().oauth2(getAdminAccessToken())
                .contentType("application/json")
                .body(JsonSerialization.writeValueAsBytes(realm))
                .when()
                .post(KEYCLOAK_SERVER_URL + "/admin/realms").then()
                .statusCode(201);
    }

    @AfterAll
    public static void removeKeycloakRealm() {
        RestAssured
                .given()
                .auth().oauth2(getAdminAccessToken())
                .when()
                .delete(KEYCLOAK_SERVER_URL + "/admin/realms/" + KEYCLOAK_REALM).thenReturn().prettyPrint();
    }

    private static String getAdminAccessToken() {
        return RestAssured
                .given()
                .param("grant_type", "password")
                .param("username", "admin")
                .param("password", "admin")
                .param("client_id", "admin-cli")
                .when()
                .post(KEYCLOAK_SERVER_URL + "/realms/master/protocol/openid-connect/token")
                .as(AccessTokenResponse.class).getToken();
    }

    private static RealmRepresentation createRealm(String name) {
        RealmRepresentation realm = new RealmRepresentation();

        realm.setRealm(name);
        realm.setEnabled(true);
        realm.setUsers(new ArrayList<>());
        realm.setClients(new ArrayList<>());
        realm.setSsoSessionMaxLifespan(2); // sec
        realm.setAccessTokenLifespan(3); // 3 seconds

        RolesRepresentation roles = new RolesRepresentation();
        List<RoleRepresentation> realmRoles = new ArrayList<>();

        roles.setRealm(realmRoles);
        realm.setRoles(roles);

        realm.getRoles().getRealm().add(new RoleRepresentation("user", null, false));
        realm.getRoles().getRealm().add(new RoleRepresentation("admin", null, false));
        realm.getRoles().getRealm().add(new RoleRepresentation("confidential", null, false));

        return realm;
    }

    private static ClientRepresentation createClient(String clientId) {
        ClientRepresentation client = new ClientRepresentation();

        client.setClientId(clientId);
        client.setPublicClient(true);
        client.setDirectAccessGrantsEnabled(true);
        client.setEnabled(true);
        client.setRedirectUris(Arrays.asList("*"));

        return client;
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

    @Test
    public void testCodeFlowNoConsent() throws IOException {
        try (final WebClient webClient = new WebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/index.html");

            assertEquals("Log in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();

            assertEquals("Welcome to Test App", page.getTitleText());

            page = webClient.getPage("http://localhost:8081/index.html");

            assertEquals("Welcome to Test App", page.getTitleText(),
                    "A second request should not redirect and just re-authenticate the user");
        }
    }

    @Test
    public void testTokenTimeoutLogout() throws IOException, InterruptedException {
        try (final WebClient webClient = new WebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/index.html");

            assertEquals("Log in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();

            assertEquals("Welcome to Test App", page.getTitleText());

            Thread.sleep(5000);

            page = webClient.getPage("http://localhost:8081/index.html");

            Cookie sessionCookie = getSessionCookie(webClient);

            assertNull(sessionCookie);

            page = webClient.getPage("http://localhost:8081/index.html");

            assertEquals("Log in to quarkus", page.getTitleText());
        }
    }

    @Test
    public void testIdTokenInjection() throws IOException, InterruptedException {
        try (final WebClient webClient = new WebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/index.html");

            assertEquals("Log in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();

            assertEquals("Welcome to Test App", page.getTitleText());

            page = webClient.getPage("http://localhost:8081/web-app");

            assertEquals("alice", page.getBody().asText());
        }
    }

    @Test
    //@Disabled
    public void testAccessTokenInjection() throws IOException, InterruptedException {
        try (final WebClient webClient = new WebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/index.html");

            assertEquals("Log in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();

            assertEquals("Welcome to Test App", page.getTitleText());

            page = webClient.getPage("http://localhost:8081/web-app/access");

            assertEquals("AT injected", page.getBody().asText());
        }
    }

    @Test
    public void testAccessAndRefreshTokenInjection() throws IOException, InterruptedException {
        try (final WebClient webClient = new WebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/index.html");

            assertEquals("Log in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getInputByName("login").click();

            assertEquals("Welcome to Test App", page.getTitleText());

            page = webClient.getPage("http://localhost:8081/web-app/refresh");

            assertEquals("RT injected", page.getBody().asText());
        }
    }

    private Cookie getSessionCookie(WebClient webClient) {
        return webClient.getCookieManager().getCookie("q_session");
    }
}
