package io.quarkus.funqy.runtime.bindings.knative.events;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.funqy.knative-events")
public interface FunqyKnativeEventsConfig {

    @ConfigGroup
    interface FunctionMapping {
        /**
         * Cloud Event type (ce-type) that triggers this function.
         * Default value is function name.
         *
         * This config item is only required when there is more than one function defined
         * within the deployment. The ce-type is not looked at if there is only one function
         * in the deployment. The message will just be dispatched to that function. This allows
         * you to change the knative trigger binding without having to change the configuration
         * of the quarkus deployment.
         */
        Optional<String> trigger();

        /**
         * If function has response output, then what is the Cloud Event type (ce-type)?
         * This will default to {function}.output
         */
        Optional<String> responseType();

        /**
         * If function has response output, then what is the Cloud Event source (ce-source)?
         * This will default to the function name
         */
        Optional<String> responseSource();
    }

    /**
     * Cloud event to function mapping. Key to this map is a function name.
     */
    Map<String, FunctionMapping> mapping();

}
