package io.quarkus.qute.deployment.typesafe;

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

}
