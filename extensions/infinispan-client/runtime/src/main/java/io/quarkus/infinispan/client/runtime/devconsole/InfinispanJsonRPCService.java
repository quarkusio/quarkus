package io.quarkus.infinispan.client.runtime.devconsole;

import jakarta.inject.Singleton;

import io.quarkus.arc.Arc;
import io.quarkus.arc.Unremovable;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.logging.Log;
import io.smallrye.common.annotation.NonBlocking;

@IfBuildProfile("dev")
@Unremovable
@Singleton
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
