package io.quarkus.smallrye.reactivemessaging.runtime.dev.ui;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class ReactiveMessagingJsonRpcService {

    public JsonArray getInfo() {
        JsonArray result = new JsonArray();

        List<DevReactiveMessagingInfos.DevChannelInfo> channels = new DevReactiveMessagingInfos().getChannels();
        for (DevReactiveMessagingInfos.DevChannelInfo channel : channels) {
            JsonObject channelAsJson = toJson(channel);
            result.add(channelAsJson);
        }
        return result;
    }

    private JsonObject toJson(DevReactiveMessagingInfos.DevChannelInfo channel) {
        JsonObject json = new JsonObject();
        json.put("name", channel.getName());
        json.put("publishers", toJson(channel.getPublishers()));
        json.put("consumers", toJson(channel.getConsumers()));
        return json;
    }

    private JsonArray toJson(List<DevReactiveMessagingInfos.Component> components) {
        JsonArray array = new JsonArray();
        for (DevReactiveMessagingInfos.Component component : components) {
            array.add(toJson(component));
        }
        return array;
    }

    private JsonObject toJson(DevReactiveMessagingInfos.Component component) {
        JsonObject json = new JsonObject();
        json.put("type", component.type.toString());
        json.put("description", component.description);
        json.put("isConnector", component.isConnector());
        return json;
    }
}
