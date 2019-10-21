package io.quarkus.vault.runtime;

import static io.quarkus.vault.runtime.LogConfidentialityLevel.LOW;

public class VaultToken extends TimeLimitedBase {

    public String clientToken;

    public VaultToken(String clientToken, boolean renewable, long leaseDurationSecs) {
        super(renewable, leaseDurationSecs);
        this.clientToken = clientToken;
    }

    public String getConfidentialInfo(LogConfidentialityLevel level) {
        return "{clientToken: " + level.maskWithTolerance(clientToken, LOW) + ", " + super.info() + "}";
    }

}
