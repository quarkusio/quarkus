package io.quarkus.amazon.lambda.http;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.lambda-http")
public interface LambdaHttpConfig {

    /**
     * If true, Quarkus will map claims from Cognito to Quarkus security roles.
     * The "cognito:groups" claim will be used by default. Change cognitoRoleClaim
     * config value to change the claim source.
     * <p>
     * True by default
     */
    @WithDefault("true")
    boolean mapCognitoToRoles();

    /**
     * Cognito claim that contains roles you want to map. Defaults to "cognito:groups"
     */
    @WithDefault("cognito:groups")
    String cognitoRoleClaim();

    /**
     * Regular expression to locate role values within a Cognito claim string.
     * By default, it looks for space delimited strings enclosed in brackets
     * "[^\[\] \t]+"
     */
    @WithDefault(value = "[^\\[\\] \\t]+")
    String cognitoClaimMatcher();

}
