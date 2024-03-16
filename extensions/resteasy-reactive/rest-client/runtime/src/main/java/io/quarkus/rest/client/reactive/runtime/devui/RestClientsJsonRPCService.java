package io.quarkus.rest.client.reactive.runtime.devui;

import java.util.Comparator;

import jakarta.inject.Singleton;

import io.smallrye.common.annotation.NonBlocking;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@Singleton
public class RestClientsJsonRPCService {

    private final RestClientsContainer restClientsContainer;

    public RestClientsJsonRPCService(RestClientsContainer restClientsContainer) {
        this.restClientsContainer = restClientsContainer;
    }

    @NonBlocking
    public JsonArray getAll() {
        var allClients = restClientsContainer.getClientData().clients;
        allClients.sort(Comparator.comparing(rci -> rci.interfaceClass));

        var result = new JsonArray();
        for (RestClientsContainer.RestClientInfo rci : allClients) {
            result.add(new JsonObject()
                    .put("clientInterface", rci.interfaceClass)
                    .put("isBean", rci.isBean)
                    .put("configKey", rci.configKey));
        }
        return result;
    }

}
