package io.quarkus.vault.runtime;

public enum LogConfidentialityLevel {

    /**
     * Display all secrets.
     * <p>
     * This may be appropriate for development environments.
     */
    LOW,
    /**
     * Display only usernames and lease ids (ie: passwords and token are masked).
     */
    MEDIUM,
    /**
     * Mask all secrets.
     */
    HIGH;

    /**
     * Mask the argument if the tolerance level is lower or equal to the receiver (which would typically represent
     * the actual configuration). so for instance passwords and auth tokens would have a LOW tolerance, whereas
     * lease ids and usernames would have a MEDIUM tolerance. As a result usernames would be shown and passwords
     * would be hidden if the system was configured with a MEDIUM level.
     */
    public String maskWithTolerance(String info, LogConfidentialityLevel infoToleranceLevel) {
        return ordinal() <= infoToleranceLevel.ordinal() ? info : "***";
    }

}
