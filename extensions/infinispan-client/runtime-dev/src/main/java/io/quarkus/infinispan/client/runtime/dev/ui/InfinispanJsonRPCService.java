package io.quarkus.infinispan.client.runtime.dev.ui;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.smallrye.common.annotation.NonBlocking;

public class InfinispanJsonRPCService {

    private static final Logger LOG = Logger.getLogger(InfinispanJsonRPCService.class);

    @NonBlocking
    public String getConsoleDefaultLink() {
        InfinispanClientsContainer clientsContainer = Arc.container().instance(InfinispanClientsContainer.class).get();
        if (clientsContainer != null) {
            return "http://" + clientsContainer.clientsInfo().get(0).serverUrl + "/console";
        }
        return "";
    }
}
