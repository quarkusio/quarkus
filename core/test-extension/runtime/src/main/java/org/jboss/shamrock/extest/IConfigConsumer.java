package org.jboss.shamrock.extest;

/**
 * Interface used to pass the runtime configuration to an application bean
 */
public interface IConfigConsumer {
    void loadConfig(TestBuildTimeConfig buildTimeConfig, TestRunTimeConfig runTimeConfig);
}
