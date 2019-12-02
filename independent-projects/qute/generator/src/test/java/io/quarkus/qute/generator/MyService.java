package io.quarkus.qute.generator;

import io.quarkus.qute.TemplateData;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@TemplateData(ignore = "surname")
public class MyService {

    private final String name = "Foo";

    int age = 10;

    public String surname;

    public String getName() {
        return name;
    }

    public Map<?, ?> getMap() {
        return null;
    }

    public boolean isActive() {
        return true;
    }

    public List<String> getList(int limit, String dummy) {
        AtomicInteger idx = new AtomicInteger(0);
        return Stream.generate(() -> "" + idx.getAndIncrement())
                .limit(limit).collect(Collectors.toList());
    }

    public List<String> getList(int limit) {
        AtomicInteger idx = new AtomicInteger(0);
        return Stream.generate(() -> "" + idx.getAndIncrement())
                .limit(limit).collect(Collectors.toList());
    }

    public CompletionStage<String> getTestName() {
        return CompletableFuture.completedFuture("oof");
    }

    public CompletionStage<String> getAnotherTestName(String param) {
        return CompletableFuture.completedFuture(param);
    }

}
