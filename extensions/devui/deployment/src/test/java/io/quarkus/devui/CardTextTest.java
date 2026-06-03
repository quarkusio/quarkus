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

import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.CardText;

public class CardTextTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testStaticTextBuilder() {
        CardText text = CardText.textBuilder()
                .title("Version")
                .icon("font-awesome-solid:tag")
                .staticText("2.1.0")
                .build();

        assertEquals("Version", text.getTitle());
        assertEquals("font-awesome-solid:tag", text.getIcon());
        assertEquals("2.1.0", text.getStaticText());
        assertNull(text.getDynamicText());
        assertNull(text.getStreamingText());
        assertNull(text.getStreamingTextParams());
    }

    @Test
    public void testDynamicTextBuilder() {
        CardText text = CardText.textBuilder()
                .title("Status")
                .dynamicText("getStatus")
                .build();

        assertEquals("Status", text.getTitle());
        assertEquals("getStatus", text.getDynamicText());
        assertNull(text.getStaticText());
        assertNull(text.getStreamingText());
    }

    @Test
    public void testStreamingTextBuilder() {
        CardText text = CardText.textBuilder()
                .title("Connections")
                .icon("font-awesome-solid:link")
                .streamingText("streamConnectionCount")
                .streamingTextParams("param1", "param2")
                .build();

        assertEquals("Connections", text.getTitle());
        assertEquals("font-awesome-solid:link", text.getIcon());
        assertEquals("streamConnectionCount", text.getStreamingText());
        assertEquals("param1,param2", text.getStreamingTextParams());
        assertNull(text.getStaticText());
        assertNull(text.getDynamicText());
    }

    @Test
    public void testBuilderRequiresTextSource() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            CardText.textBuilder()
                    .title("No Text")
                    .build();
        });
        assertTrue(ex.getMessage().contains("staticText"));
    }

    @Test
    public void testTitleIsOptional() {
        CardText text = CardText.textBuilder()
                .staticText("just text")
                .build();

        assertNull(text.getTitle());
        assertEquals("just text", text.getStaticText());
    }

    @Test
    public void testCardPageBuildItemTexts() {
        CardPageBuildItem item = new CardPageBuildItem();
        assertFalse(item.hasCardTexts());
        assertTrue(item.getCardTexts().isEmpty());

        CardText text1 = CardText.textBuilder()
                .title("Text 1")
                .staticText("value1")
                .build();
        CardText text2 = CardText.textBuilder()
                .title("Text 2")
                .dynamicText("getValue2")
                .build();

        item.addText(text1);
        item.addText(text2);

        assertTrue(item.hasCardTexts());
        List<CardText> texts = item.getCardTexts();
        assertEquals(2, texts.size());
        assertEquals("Text 1", texts.get(0).getTitle());
        assertEquals("Text 2", texts.get(1).getTitle());
    }

    @Test
    public void testJacksonSerialization() throws Exception {
        CardText text = CardText.textBuilder()
                .title("Active Sessions")
                .icon("font-awesome-solid:users")
                .staticText("42")
                .build();

        String json = mapper.writeValueAsString(text);
        JsonNode node = mapper.readTree(json);

        assertEquals("Active Sessions", node.get("title").asText());
        assertEquals("font-awesome-solid:users", node.get("icon").asText());
        assertEquals("42", node.get("staticText").asText());
        assertTrue(node.get("dynamicText").isNull());
        assertTrue(node.get("streamingText").isNull());
        assertTrue(node.get("streamingTextParams").isNull());
    }

    @Test
    public void testJacksonSerializationDynamic() throws Exception {
        CardText text = CardText.textBuilder()
                .dynamicText("fetchCount")
                .build();

        String json = mapper.writeValueAsString(text);
        JsonNode node = mapper.readTree(json);

        assertEquals("fetchCount", node.get("dynamicText").asText());
        assertTrue(node.get("title").isNull());
        assertTrue(node.get("icon").isNull());
        assertTrue(node.get("staticText").isNull());
        assertTrue(node.get("streamingText").isNull());
    }
}
