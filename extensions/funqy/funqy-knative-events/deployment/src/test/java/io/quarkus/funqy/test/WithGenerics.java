package io.quarkus.funqy.test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEventMapping;
import io.smallrye.mutiny.Uni;

public class WithGenerics {

    @Funq
    @CloudEventMapping(trigger = "listOfStrings")
    public String toCommaSeparated(List<Identity> identityList) {
        return identityList
                .stream()
                .map(Identity::getName)
                .collect(Collectors.joining(","));
    }

    @Funq
    @CloudEventMapping(trigger = "integer")
    public Uni<List<Integer>> range(int n) {
        return Uni.createFrom().item(() -> IntStream.range(0, n)
                .boxed()
                .collect(Collectors.toList()));
    }
}
