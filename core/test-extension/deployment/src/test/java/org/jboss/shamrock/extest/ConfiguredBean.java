package org.jboss.shamrock.extest;

import javax.enterprise.event.Observes;

import org.jboss.shamrock.runtime.StartupEvent;

/**
 * A sample bean
 */
@TestAnnotation
public class ConfiguredBean implements IRTConfig {
    TestRunTimeConfig runTimeConfig;

   /**
     * Called by runtime with the runtime config object
     * @param runTimeConfig
     */
    @Override
    public void loadConfig(TestRunTimeConfig runTimeConfig) {
        System.out.printf("loadConfig, runTimeConfig=%s\n", runTimeConfig);
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
