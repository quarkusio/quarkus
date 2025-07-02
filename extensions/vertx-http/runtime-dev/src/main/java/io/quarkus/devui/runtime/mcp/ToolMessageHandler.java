package io.quarkus.devui.runtime.mcp;

import java.util.Map;

import org.jboss.logging.Logger;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ToolMessageHandler extends MessageHandler {

    private static final Logger LOG = Logger.getLogger(ToolMessageHandler.class);

    Future<Void> toolsList(JsonObject message, McpRequest mcpRequest) {
        Object id = message.getValue("id");
        Cursor cursor = Messages.getCursor(message, mcpRequest.sender());
        if (cursor == null) {
            return Future.succeededFuture();
        }

        LOG.debugf("List tools [id: %s, cursor: %s]", id, cursor);

        int pageSize = 50;

        JsonArray tools = new JsonArray();

        JsonObject exampleTool = new JsonObject()
                .put("name", "getAlerts")
                .put("description", "Get weather alerts for a state");
        JsonObject properties = new JsonObject();
        JsonArray required = new JsonArray();
        properties.put("state",
                new JsonObject().put("type", "string").put("description", "Two-letter US state code (e.g. CA, NY)"));
        required.add("state");
        exampleTool.put("inputSchema", new JsonObject()
                .put("type", "object")
                .put("properties", properties)
                .put("required", required));

        tools.add(exampleTool);

        return mcpRequest.sender().sendResult(id, new JsonObject().put("tools", tools));
    }

    Future<Void> toolsCall(JsonObject message, McpRequest mcpRequest) {
        Object id = message.getValue("id");
        JsonObject params = message.getJsonObject("params");
        String toolName = params.getString("name");
        LOG.debugf("Call tool %s [id: %s]", toolName, id);

        Map<String, Object> args = params.containsKey("arguments") ? params.getJsonObject("arguments").getMap() : Map.of();

        try {
            Future<ToolResponse> exampleResponse = Future.succeededFuture(ToolResponse.success(
                    "\"text\":\"Event: Red Flag Warning\\nArea: Modoc County Except for the Surprise Valley; Klamath Basin and the Fremont-Winema National Forest; South Central Oregon Desert including the BLM Land in Eastern Lake and Western Harney Counties\\nSeverity: Severe\\nDescription: The National Weather Service in Medford has issued a Red Flag\\nWarning, which is in effect from 2 PM to 8 PM PDT Wednesday.\\n\\n* IMPACTS...Any fires that develop will likely spread rapidly.\\n\\n* AFFECTED AREA...In CAZ285...Fire weather zone 285.In ORZ624...\\nFire weather zone 624.In ORZ625...Fire weather zone 625.\\n\\n* THUNDERSTORMS...Scattered afternoon and evening thunderstorms,\\nmainly along and eats of the highway 97 corridor.\\n\\n* OUTFLOW WINDS...Gusts up to 45 mph. These outflow winds can\\ntravel up to 25 miles away from the thunderstorm that caused it.\\n\\n* DETAILEDURL...View the hazard area in detail at\\nhttps://www.wrh.noaa.gov/map/?wfo=mfr\\nInstructions: Follow all fire restrictions. You can find your county's\\nemergency sign up form as well as links to fire restrictions at\\nweather.gov/medford/wildfire. One less spark, one less wildfire.\\n\\nBe sure you're signed up for your county's emergency alert\\nsystem. Familiarize yourself with your emergency plan and make\\nsure you listen to emergency services. Visit ready.gov/plan for\\nmore information.\\n\\nA Red Flag Warning is issued when we identify weather conditions\\nthat promote rapid spread of fire which may become life-\\nthreatening. This does not mean there is a fire. These conditions\\nare either occurring now or will begin soon. It is important to\\nhave multiple ways to receive information from authorities.\\n\\n---\\nEvent: Red Flag Warning\\nArea: Siskiyou County from the Cascade Mountains East and South to Mt Shasta\\nSeverity: Severe\\nDescription: The National Weather Service in Medford has issued a Red Flag\\nWarning, which is in effect from 2 PM to 8 PM PDT Wednesday.\\n\\n* IMPACTS...Given the long stretch of dry and hot and very\\nconditions, lightning efficiency will be moderate for new fire\\nstarts. Any fires that develop will likely spread rapidly.\\n\\n* AFFECTED AREA...All of Fire Weather Zone 284.\\n\\n* THUNDERSTORMS...Scattered afternoon and evening thunderstorms\\nacross the region.\\n\\n* OUTFLOW WINDS...Gusts up to 45 mph. These outflow winds can\\ntravel up to 25 miles away from the thunderstorm that caused it.\\n\\n* ADDITIONAL INFORMATION...Some storms could be dry with gusty\\noutflows.\\n\\n* DETAILED URL...View the hazard area in detail at\\nhttps://www.wrh.noaa.gov/map/?wfo=mfr\\nInstructions: Follow all fire restrictions. You can find your county's\\nemergency sign up form as well as links to fire restrictions at\\nweather.gov/medford/wildfire. One less spark, one less wildfire.\\n\\nBe sure you're signed up for your county's emergency alert\\nsystem. Familiarize yourself with your emergency plan and make\\nsure you listen to emergency services. Visit ready.gov/plan for\\nmore information.\\n\\nA Red Flag Warning is issued when we identify weather conditions\\nthat promote rapid spread of fire which may become life-\\nthreatening. This does not mean there is a fire. These conditions\\nare either occurring now or will begin soon. It is important to\\nhave multiple ways to receive information from authorities.\\n\""));
            Future<ToolResponse> fu = exampleResponse; // TODO: actually execute the call
            return fu.compose(toolResponse -> {
                JsonArray contents = new JsonArray();
                for (Content content : toolResponse.content()) {
                    switch (content.type()) {
                        case TEXT -> contents.add(new JsonObject().put("type", "text").put("text", content.asText().text()));
                        default -> {
                        } // TODO: fill in the rest
                    }
                }
                return mcpRequest.sender().sendResult(id,
                        new JsonObject()
                                .put("isError", toolResponse.isError())
                                .put("content", contents));
            }, cause -> {
                if (cause instanceof ToolCallException tce) {
                    // Business logic error should result in ToolResponse with isError:true
                    return mcpRequest.sender().sendResult(id, ToolResponse.error(tce.getMessage()));
                } else {
                    return handleFailure(id, mcpRequest.sender(), mcpRequest.connection(), cause, LOG,
                            "Unable to call tool %s", toolName);
                }
            });
        } catch (McpException e) {
            return mcpRequest.sender().sendError(id, e.getJsonRpcError(), e.getMessage());
        }
    }

}
