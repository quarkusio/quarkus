package io.quarkus.amazon.lambda.http;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public class LambdaHttpConfig {

    /**
     * If true, runtime will search Cognito JWT claims for "cognito:groups"
     * and add them as Quarkus security roles.
     *
     * True by default
     */
    @ConfigItem(defaultValue = "true")
    public boolean mapCognitoToRoles;

    /**
     * Cognito claim that contains roles you want to map. Defaults to "cognito:groups"
     */
    @ConfigItem(defaultValue = "cognito:groups")
    public String cognitoRoleClaim;

    /**
     * Regular expression to locate role values within a Cognito claim string.
     * By default it looks for space delimited strings enclosed in brackets
     * "[^\[\] \t]+"
     */
    @ConfigItem(defaultValue = "[^\\[\\] \\t]+")
    public String cognitoClaimMatcher;
}
