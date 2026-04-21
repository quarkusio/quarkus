package io.quarkus.devui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.devui.spi.page.CardAction;
import io.quarkus.devui.spi.page.CardAction.ActionType;
import io.quarkus.devui.spi.page.CardPageBuildItem;

public class CardActionTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testJsonRpcActionBuilder() {
        CardAction action = CardAction.actionBuilder()
                .title("Clean All")
                .icon("font-awesome-solid:broom")
                .tooltip("Clean all databases")
                .jsonRpcMethodName("cleanAll")
                .build();

        assertEquals("Clean All", action.getTitle());
        assertEquals("font-awesome-solid:broom", action.getIcon());
        assertEquals("Clean all databases", action.getTooltip());
        assertEquals(ActionType.JSONRPC, action.getActionType());
        assertEquals("cleanAll", action.getActionReference());
        assertTrue(action.isShowResultNotification());
    }

    @Test
    public void testUrlActionBuilder() {
        CardAction action = CardAction.actionBuilder()
                .title("Refresh")
                .url("/q/refresh")
                .showResultNotification(false)
                .build();

        assertEquals("Refresh", action.getTitle());
        assertEquals(ActionType.URL, action.getActionType());
        assertEquals("/q/refresh", action.getActionReference());
        assertFalse(action.isShowResultNotification());
        assertNull(action.getIcon());
        assertNull(action.getTooltip());
    }

    @Test
    public void testBuilderRequiresTitle() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            CardAction.actionBuilder()
                    .jsonRpcMethodName("doSomething")
                    .build();
        });
        assertTrue(ex.getMessage().contains("title"));
    }

    @Test
    public void testBuilderRequiresAction() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            CardAction.actionBuilder()
                    .title("No Action")
                    .build();
        });
        assertTrue(ex.getMessage().contains("jsonRpcMethodName"));
    }

    @Test
    public void testCardPageBuildItemActions() {
        CardPageBuildItem item = new CardPageBuildItem();
        assertFalse(item.hasCardActions());
        assertTrue(item.getCardActions().isEmpty());

        CardAction action1 = CardAction.actionBuilder()
                .title("Action 1")
                .jsonRpcMethodName("method1")
                .build();
        CardAction action2 = CardAction.actionBuilder()
                .title("Action 2")
                .url("/api/action2")
                .build();

        item.addAction(action1);
        item.addAction(action2);

        assertTrue(item.hasCardActions());
        List<CardAction> actions = item.getCardActions();
        assertEquals(2, actions.size());
        assertEquals("Action 1", actions.get(0).getTitle());
        assertEquals("Action 2", actions.get(1).getTitle());
    }

    @Test
    public void testJacksonSerialization() throws Exception {
        CardAction action = CardAction.actionBuilder()
                .title("Generate Report")
                .icon("font-awesome-solid:wand-magic-sparkles")
                .tooltip("Generate a report")
                .jsonRpcMethodName("generateReport")
                .showResultNotification(true)
                .build();

        String json = mapper.writeValueAsString(action);
        JsonNode node = mapper.readTree(json);

        assertEquals("Generate Report", node.get("title").asText());
        assertEquals("font-awesome-solid:wand-magic-sparkles", node.get("icon").asText());
        assertEquals("Generate a report", node.get("tooltip").asText());
        assertEquals("JSONRPC", node.get("actionType").asText());
        assertEquals("generateReport", node.get("actionReference").asText());
        assertTrue(node.get("showResultNotification").asBoolean());
    }

    @Test
    public void testJacksonSerializationWithNullOptionalFields() throws Exception {
        CardAction action = CardAction.actionBuilder()
                .title("Simple")
                .url("/api/simple")
                .build();

        String json = mapper.writeValueAsString(action);
        JsonNode node = mapper.readTree(json);

        assertEquals("Simple", node.get("title").asText());
        assertEquals("URL", node.get("actionType").asText());
        assertEquals("/api/simple", node.get("actionReference").asText());
        assertTrue(node.get("icon").isNull());
        assertTrue(node.get("tooltip").isNull());
    }
}
