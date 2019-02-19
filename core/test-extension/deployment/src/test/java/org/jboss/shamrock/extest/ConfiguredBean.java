package org.jboss.shamrock.extest;

import javax.enterprise.event.Observes;

import org.jboss.shamrock.runtime.StartupEvent;

/**
 * A sample bean
 */
@TestAnnotation
public class ConfiguredBean implements IConfigConsumer {
    TestRunTimeConfig runTimeConfig;
    TestBuildTimeConfig buildTimeConfig;

   /**
     * Called by runtime with the runtime config object
     * @param runTimeConfig
     */
    @Override
    public void loadConfig(TestBuildTimeConfig buildTimeConfig, TestRunTimeConfig runTimeConfig) {
        System.out.printf("loadConfig, buildTimeConfig=%s, runTimeConfig=%s\n", buildTimeConfig, runTimeConfig);
        this.buildTimeConfig = buildTimeConfig;
        this.runTimeConfig = runTimeConfig;
    }

    /**
     * Called when the runtime has started
     * @param event
     */
    void onStart(@Observes StartupEvent event) {
        System.out.printf("onStart, event=%s\n", event);
    }

    @Override
    public String toString() {
        return "ConfiguredBean{" +
                "runTimeConfig=" + runTimeConfig +
                '}';
    }
}
