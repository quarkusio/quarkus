package io.quarkus.it.quartz;

import org.quartz.SchedulerException;
import org.quartz.spi.InstanceIdGenerator;

public class FixedInstanceIdGenerator implements InstanceIdGenerator {

    private String instanceId;

    @Override
    public String generateInstanceId() throws SchedulerException {
        return instanceId;
    }

    public String getInstanceId() {
        return this.instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

}
