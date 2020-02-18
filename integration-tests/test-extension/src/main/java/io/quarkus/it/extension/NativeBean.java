package io.quarkus.it.extension;

import javax.enterprise.event.Observes;

import io.quarkus.extest.runtime.IConfigConsumer;
import io.quarkus.extest.runtime.TestAnnotation;
import io.quarkus.extest.runtime.config.FooRuntimeConfig;
import io.quarkus.extest.runtime.config.TestBuildAndRunTimeConfig;
import io.quarkus.extest.runtime.config.TestRunTimeConfig;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@TestAnnotation
public class NativeBean implements IConfigConsumer {
    volatile TestRunTimeConfig runTimeConfig;
    volatile TestBuildAndRunTimeConfig buildTimeConfig;
    volatile FooRuntimeConfig fooRuntimeConfig;

    public NativeBean() {
        System.out.printf("NativeBean.ctor, %s%n", super.toString());
    }

    /**
     * Called by runtime with the runtime config object
     *
     * @param runTimeConfig
     */
    @Override
    public void loadConfig(TestBuildAndRunTimeConfig buildTimeConfig, TestRunTimeConfig runTimeConfig,
            FooRuntimeConfig fooRuntimeConfig) {
        System.out.printf("loadConfig, buildTimeConfig=%s, runTimeConfig=%s, fooRuntimeConfig=%s%n", buildTimeConfig,
                runTimeConfig, fooRuntimeConfig);
        this.buildTimeConfig = buildTimeConfig;
        this.runTimeConfig = runTimeConfig;
        this.fooRuntimeConfig = fooRuntimeConfig;
    }

    /**
     * Called when the runtime has started
     *
     * @param event
     */
    void onStart(@Observes StartupEvent event) {
        System.out.printf("onStart, event=%s%n", event);
    }

    void onStop(@Observes ShutdownEvent event) {
        System.out.printf("onStop, event=%s%n", event);
    }

}
