package io.quarkus.infinispan.client.runtime.dev.ui;

import io.quarkus.arc.Arc;
import io.quarkus.logging.Log;
import io.smallrye.common.annotation.NonBlocking;

public class InfinispanJsonRPCService {

    @NonBlocking
    public String getConsoleDefaultLink() {
        InfinispanClientsContainer clientsContainer = Arc.container().instance(InfinispanClientsContainer.class).get();
        if (clientsContainer != null) {
            Log.info(clientsContainer.clientsInfo().get(0).serverUrl);
            return "http://" + clientsContainer.clientsInfo().get(0).serverUrl + "/console";
        }
        return "";
    }
}
