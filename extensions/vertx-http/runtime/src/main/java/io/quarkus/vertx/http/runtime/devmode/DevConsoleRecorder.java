package io.quarkus.vertx.http.runtime.devmode;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class DevConsoleRecorder {

    public void addInfo(String groupId, String artifactId, String name, Supplier<? extends Object> supplier) {
        Map<String, Map<String, Object>> info = DevConsoleManager.getTemplateInfo();
        Map<String, Object> data = info.computeIfAbsent(groupId + "." + artifactId,
                new Function<String, Map<String, Object>>() {
                    @Override
                    public Map<String, Object> apply(String s) {
                        return new HashMap<>();
                    }
                });
        data.put(name, supplier.get());
    }
}
