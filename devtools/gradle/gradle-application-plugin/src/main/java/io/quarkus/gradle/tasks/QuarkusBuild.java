package io.quarkus.gradle.tasks;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.TaskAction;

import io.quarkus.gradle.dsl.Manifest;

@CacheableTask
public abstract class QuarkusBuild extends QuarkusAugmentTask {

    @Inject
    public QuarkusBuild() {
        super("Quarkus builds a runner jar based on the build jar");
    }

    public QuarkusBuild nativeArgs(Action<Map<String, ?>> action) {
        Map<String, ?> nativeArgsMap = new HashMap<>();
        action.execute(nativeArgsMap);
        for (Map.Entry<String, ?> nativeArg : nativeArgsMap.entrySet()) {
            System.setProperty(expandConfigurationKey(nativeArg.getKey()), nativeArg.getValue().toString());
        }
        return this;
    }

    public QuarkusBuild manifest(Action<Manifest> action) {
        action.execute(this.getManifest());
        return this;
    }

    @TaskAction
    public void buildQuarkus() {
        withAugmentAction(action -> action.createProductionApplication());
    }
}
