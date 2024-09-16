package io.quarkus.devui;

import static java.util.stream.StreamSupport.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;

public class ConfigurationSinglePropertyUpdatesRPCTest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot(
                    jar -> jar
                            .addAsResource(
                                    "conf/devui-configuration-test.properties",
                                    "application.properties"));

    public ConfigurationSinglePropertyUpdatesRPCTest() {
        super("devui-configuration");
    }

    static AbstractListAssert<?, List<? extends String>, String, ObjectAssert<String>> assertThatResponseDoesNotContainProperty(
            JsonNode response, String name) {
        assertThat(response)
                .isNotNull()
                .isInstanceOf(ArrayNode.class);
        final var projectProperties = (ArrayNode) response;
        return assertThat(stream(projectProperties.spliterator(), false))
                .isNotEmpty()
                .extracting(node -> node.get("key").asText())
                .doesNotContain(name);
    }

    AbstractListAssert<?, List<? extends String>, String, ObjectAssert<String>> assertThatProjectDoesNotHaveProperty(
            String name) throws Exception {
        final var projectPropertiesResponse = super.executeJsonRPCMethod("getProjectProperties");
        return assertThatResponseDoesNotContainProperty(
                projectPropertiesResponse,
                name);
    }

    static AbstractListAssert<?, List<? extends Tuple>, Tuple, ObjectAssert<Tuple>> assertThatResponseContainsProperty(
            JsonNode response, String name, String value) {
        assertThat(response)
                .isNotNull()
                .isInstanceOf(ArrayNode.class);
        final var projectProperties = (ArrayNode) response;
        return assertThat(stream(projectProperties.spliterator(), false))
                .isNotEmpty()
                .extracting(node -> node.get("key").asText(), node -> node.get("value").asText())
                .contains(tuple(name, value));
    }

    AbstractListAssert<?, List<? extends Tuple>, Tuple, ObjectAssert<Tuple>> assertThatProjectHasProperty(String name,
            String value) throws Exception {
        final var projectPropertiesResponse = super.executeJsonRPCMethod("getProjectProperties");
        return assertThatResponseContainsProperty(
                projectPropertiesResponse,
                name,
                value);
    }

    void updateProperty(String name, String value) throws Exception {
        final var response = super.executeJsonRPCMethod(
                "updateProperty",
                Map.of(
                        "name", name,
                        "value", value));
        assertThat(response)
                .isNotNull();
        assertThat(response.asBoolean())
                .isTrue();
    }

    @Test
    void testSingleConfigurationProperty() throws Exception {
        assertThatProjectDoesNotHaveProperty("x.y");
        updateProperty(
                "x.y",
                "changedByTest");
        assertThatProjectHasProperty(
                "x.y",
                "changedByTest");
    }

    @Test
    void testUpdateExistingProperty() throws Exception {
        updateProperty(
                "quarkus.application.name",
                "changedByTest");
        assertThatProjectHasProperty(
                "quarkus.application.name",
                "changedByTest").doesNotHaveDuplicates();
    }

    @Test
    void testUpdateNewProperty() throws Exception {
        updateProperty(
                "quarkus.application.name",
                "changedByTest");
        assertThatProjectHasProperty(
                "quarkus.application.name",
                "changedByTest").doesNotHaveDuplicates();
    }

    @Test
    void testUpdatePropertyWithSpaces() throws Exception {
        updateProperty(
                "my.property.with.spaces",
                "changedByTest");
        assertThatProjectHasProperty(
                "my.property.with.spaces",
                "changedByTest").doesNotHaveDuplicates();
    }

    @Test
    void testUpdatePropertyWithLineBreaks() throws Exception {
        assertThatProjectHasProperty(
                "my.property.with.linebreak",
                "valuewithlinebreak");
        updateProperty(
                "my.property.with.linebreak",
                "changedByTest");
        assertThatProjectHasProperty(
                "my.property.with.linebreak",
                "changedByTest").doesNotHaveDuplicates();
    }

}
