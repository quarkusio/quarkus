package io.quarkus.funqy.test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEventMapping;
import io.quarkus.funqy.knative.events.EventAttribute;
import io.smallrye.mutiny.Uni;

public class WithAttributeFilter {

    @Funq
    @CloudEventMapping(trigger = "listOfStrings", attributes = { @EventAttribute(name = "source", value = "testvalue") })
    public String toCommaSeparated(List<Identity> identityList) {
        return identityList
                .stream()
                .map(Identity::getName)
                .collect(Collectors.joining(","));
    }

    @Funq
    @CloudEventMapping(trigger = "listOfStrings", attributes = { @EventAttribute(name = "source", value = "test2") })
    public String toSemicolonSeparated(List<Identity> identityList) {
        return identityList
                .stream()
                .map(Identity::getName)
                .collect(Collectors.joining(";"));
    }

    @Funq
    @CloudEventMapping(trigger = "integer", attributes = { @EventAttribute(name = "source", value = "test"),
            @EventAttribute(name = "custom", value = "hello") })
    public Uni<List<Integer>> range(int n) {
        return Uni.createFrom().item(() -> IntStream.range(0, n)
                .boxed()
                .collect(Collectors.toList()));
    }

    @Funq
    @CloudEventMapping(trigger = "listOfStrings", attributes = {
            @EventAttribute(name = "source", value = "test"),
            @EventAttribute(name = "custom", value = "value") })
    public String foo(List<Identity> identityList) {
        return "value";
    }

    @Funq
    @CloudEventMapping(trigger = "listOfStrings", attributes = {
            @EventAttribute(name = "source", value = "test"),
            @EventAttribute(name = "custom", value = "someOtherValue") })
    public String bar(List<Identity> identityList) {
        return "someOtherValue";
    }

    @Funq
    @CloudEventMapping(trigger = "listOfStrings", attributes = {
            @EventAttribute(name = "source", value = "test"),
            @EventAttribute(name = "customA", value = "value") })
    public String anotherFoo(List<Identity> identityList) {
        return "value";
    }

    @Funq
    @CloudEventMapping(trigger = "listOfStrings", attributes = {
            @EventAttribute(name = "source", value = "test"),
            @EventAttribute(name = "customB", value = "value") })
    public String anotherBar(List<Identity> identityList) {
        return "someOtherValue";
    }
}
