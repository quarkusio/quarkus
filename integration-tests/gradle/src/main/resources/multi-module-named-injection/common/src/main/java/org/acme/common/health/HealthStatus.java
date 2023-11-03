package org.acme.common.health;


public class HealthStatus {

    private boolean healthy = true;

    public boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }
}
