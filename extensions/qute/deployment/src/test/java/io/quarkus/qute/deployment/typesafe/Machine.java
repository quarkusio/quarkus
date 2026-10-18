package io.quarkus.qute.deployment.typesafe;

import java.util.Collections;
import java.util.List;

public class Machine {

    public Integer ping = 1;

    private MachineStatus status;

    public Machine setStatus(MachineStatus status) {
        this.status = status;
        return this;
    }

    public MachineStatus getStatus() {
        return status;
    }

    public List<String> getNames(int pings) {
        return Collections.singletonList("ping");
    }

}
