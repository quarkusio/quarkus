package org.jboss.shamrock.extest;

/**
 * Interface used to pass the runtime configuration to an application bean
 */
public interface IRTConfig {
    void loadConfig(TestRunTimeConfig runTimeConfig);
}
