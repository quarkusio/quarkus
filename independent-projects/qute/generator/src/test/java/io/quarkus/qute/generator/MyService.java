package io.quarkus.qute.generator;

import io.quarkus.qute.TemplateData;
import java.util.Arrays;
import java.util.Collections;
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

    public boolean hasName() {
        return name != null;
    }

    public boolean isActive() {
        return true;
    }

    public boolean hasItems() {
        return false;
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

    public List<String> getList(String name) {
        return Collections.emptyList();
    }

    public List<String> getListVarargs(String... names) {
        return Arrays.asList(names);
    }

    public CompletionStage<String> getTestName() {
        return CompletableFuture.completedFuture("oof");
    }

    public CompletionStage<String> getAnotherTestName(String param) {
        return CompletableFuture.completedFuture(param);
    }

    public static List<String> getDummy(MyService service, int limit, String dummy) {
        return Collections.emptyList();
    }

    public static List<String> getDummy(MyService service, int limit, long dummy) {
        return Collections.singletonList("dummy");
    }

    public static List<String> getDummyVarargs(MyService service, int limit, String... dummies) {
        if (dummies.length == 0) {
            return Collections.singletonList("" + limit);
        }
        return dummies.length > limit ? Collections.emptyList() : Arrays.asList(dummies);
    }

    public static List<String> getDummyMatchAnyLike(MyService service, String name, int limit) {
        return Collections.emptyList();
    }

    public static List<String> getDummyNamespaceLike(String name, int... limit) {
        return Collections.emptyList();
    }

    public static List<String> getDummyNamespaceRegexLike(String name, String names) {
        return Collections.emptyList();
    }

    public static String quark() {
        return "ok";
    }

    public static int getDummy(int val) {
        return val * 2;
    }

}
