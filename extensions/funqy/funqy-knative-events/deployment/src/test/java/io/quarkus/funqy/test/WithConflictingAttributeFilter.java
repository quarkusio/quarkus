package io.quarkus.funqy.test;

import java.util.List;

import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEventMapping;
import io.quarkus.funqy.knative.events.EventAttribute;

public class WithConflictingAttributeFilter {

    @Funq
    @CloudEventMapping(trigger = "listOfStrings")
    public String foo(List<Identity> identityList) {
        return "";
    }

    @Funq
    @CloudEventMapping(trigger = "listOfStrings", attributes = {
            @EventAttribute(name = "source", value = "test") })
    public String bar(List<Identity> identityList) {
        return "";
    }

}
