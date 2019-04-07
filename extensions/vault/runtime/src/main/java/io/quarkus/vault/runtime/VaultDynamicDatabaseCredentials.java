package io.quarkus.vault.runtime;

import static io.quarkus.vault.runtime.LogConfidentialityLevel.LOW;
import static io.quarkus.vault.runtime.LogConfidentialityLevel.MEDIUM;

public class VaultDynamicDatabaseCredentials extends LeaseBase {

    public String username;
    public String password;

    public VaultDynamicDatabaseCredentials(LeaseBase lease, String username, String password) {
        super(lease);
        this.username = username;
        this.password = password;
    }

    public String getConfidentialInfo(LogConfidentialityLevel level) {
        return "{" + super.getConfidentialInfo(level) + ", username: " + level.maskWithTolerance(username, MEDIUM)
                + ", password:"
                + level.maskWithTolerance(password, LOW) + "}";
    }

}
