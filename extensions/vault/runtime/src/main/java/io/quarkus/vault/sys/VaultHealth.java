package io.quarkus.vault.sys;

public class VaultHealth {

    public static final int DEFAULT_INIT_UNSEAL_ACTIVE_STATUS_CODE = 200;
    public static final int DEFAULT_UNSEAL_STANDBY_STATUS_CODE = 429;
    public static final int DEFAULT_DISASTER_RECOVERY_MODE_STATUS_CODE = 472;
    public static final int DEFAULT_PERFORMANCE_STANDBY_STATUS_CODE = 473;
    public static final int DEFAULT_NOT_INIT_STATUS_CODE = 501;
    public static final int DEFAULT_SEALED_STATUS_CODE = 503;

    private int statusCode;

    public VaultHealth(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * @return true if the Vault instance is initialized, unsealed and active.
     */
    public boolean isInitializedUnsealedActive() {
        return DEFAULT_INIT_UNSEAL_ACTIVE_STATUS_CODE == this.statusCode;
    }

    /**
     * @return true if the Vault instance is unsealed and standby.
     */
    public boolean isUnsealedStandby() {
        return DEFAULT_UNSEAL_STANDBY_STATUS_CODE == this.statusCode;
    }

    /**
     * @return true if the Vault instance is a secondary node in data recovery
     *         replication mode.
     */
    public boolean isRecoveryReplicationSecondary() {
        return DEFAULT_DISASTER_RECOVERY_MODE_STATUS_CODE == this.statusCode;
    }

    /**
     * @return true if the Vault instance is in performance standby mode.
     */
    public boolean isPerformanceStandby() {
        return DEFAULT_PERFORMANCE_STANDBY_STATUS_CODE == this.statusCode;
    }

    /**
     * @return true if the Vault instance is not initialized.
     */
    public boolean isNotInitialized() {
        return DEFAULT_NOT_INIT_STATUS_CODE == this.statusCode;
    }

    /**
     * @return true if the Vault instance is sealed.
     */
    public boolean isSealed() {
        return DEFAULT_SEALED_STATUS_CODE == this.statusCode;
    }

}
