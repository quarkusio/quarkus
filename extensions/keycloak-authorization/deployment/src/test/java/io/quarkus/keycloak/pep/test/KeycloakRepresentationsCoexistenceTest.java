package io.quarkus.keycloak.pep.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.json.KeycloakJsonMapperFactory;
import org.keycloak.json.StringOrArray;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.quarkus.keycloak.admin.resteasy.client.runtime.KeycloakAdminResteasyClientProvider;
import io.quarkus.keycloak.pep.runtime.KeycloakAdminClientJackson2Bridge;
import io.quarkus.test.QuarkusExtensionTest;
import tools.jackson.databind.json.JsonMapper;

/**
 * The Keycloak policy enforcer uses the Jackson 2 variant of the Keycloak representations while the Keycloak admin
 * client uses the Jackson 3 variant; this test makes sure that the Jackson 2 variant wins when both are present and
 * that both clients can work with it.
 * TODO: drop this test when the Keycloak policy enforcer migrates to Jackson 3
 */
public class KeycloakRepresentationsCoexistenceTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.oidc.enabled", "false")
            .overrideRuntimeConfigKey("quarkus.keycloak.admin-client.server-url", "http://localhost:1");

    @Test
    public void testJackson2RepresentationsWin() throws Exception {
        // the Jackson 2 representations use Jackson 2 JsonNode fields
        assertThat(RealmRepresentation.class.getDeclaredField("clientProfiles").getType())
                .isEqualTo(com.fasterxml.jackson.databind.JsonNode.class);
        // and carry the Jackson 2 meta-annotations the policy enforcer relies on
        assertThat(StringOrArray.class.getAnnotation(JsonDeserialize.class)).isNotNull();
        // the Jackson 3 only classes are still available
        assertThat(Class.forName("org.keycloak.json.RawJsonValue")).isNotNull();
    }

    @Test
    public void testPolicyEnforcerParsesSingleAudience() throws Exception {
        // Jackson 2, used by the policy enforcer directly
        AccessToken token = JsonSerialization.readValue("{\"aud\":\"account\",\"sub\":\"alice\"}", AccessToken.class);
        assertThat(token.getAudience()).containsExactly("account");
        // the Keycloak JSON mapper SPI resolves to the Jackson 3 mapper of the admin client, which has the priority
        token = KeycloakJsonMapperFactory.mapper().readValue("{\"aud\":\"account\",\"sub\":\"alice\"}", AccessToken.class);
        assertThat(token.getAudience()).containsExactly("account");
        assertThat(KeycloakJsonMapperFactory.mapper().getClass().getName())
                .isEqualTo("org.keycloak.admin.client.jackson3.Jackson3JsonMapper");
    }

    @Test
    public void testAdminClientBridgesJackson2JsonNode() {
        assertThat(Keycloak.getClientProvider()).isInstanceOf(KeycloakAdminResteasyClientProvider.class);

        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm("quarkus");
        realm.setParsedClientProfiles(new ClientProfilesRepresentation());

        JsonMapper mapper = KeycloakAdminClientJackson2Bridge.jsonMapper().mapper();
        String json = mapper.writeValueAsString(realm);
        assertThat(json).contains("\"clientProfiles\":{\"profiles\":[]}");
        RealmRepresentation deserialized = mapper.readValue(json, RealmRepresentation.class);
        assertThat(deserialized.getRealm()).isEqualTo("quarkus");
        assertThat(deserialized.getParsedClientProfiles()).isNotNull();
        assertThat(deserialized.getParsedClientProfiles().getProfiles()).isEmpty();
    }
}
