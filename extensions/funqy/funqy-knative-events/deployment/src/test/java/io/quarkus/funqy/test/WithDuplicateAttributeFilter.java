package io.quarkus.funqy.test;

import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEventMapping;
import io.quarkus.funqy.knative.events.EventAttribute;

public class WithDuplicateAttributeFilter {

    @Funq
    @CloudEventMapping(trigger = "listOfStrings", attributes = { @EventAttribute(name = "source", value = "test") })
    public String toCommaSeparated(List<Identity> identityList) {
        return identityList
                .stream()
                .map(Identity::getName)
                .collect(Collectors.joining(","));
    }

    @Funq
    @CloudEventMapping(trigger = "listOfStrings", attributes = { @EventAttribute(name = "source", value = "test") })
    public String toSemicolonSeparated(List<Identity> identityList) {
        return identityList
                .stream()
                .map(Identity::getName)
                .collect(Collectors.joining(";"));
    }

}
