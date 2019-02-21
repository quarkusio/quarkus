package org.jboss.shamrock.extest;

/**
 * Interface used to pass the runtime configuration to an application bean for validation
 */
public interface IConfigConsumer {
    void loadConfig(TestBuildAndRunTimeConfig buildTimeConfig, TestRunTimeConfig runTimeConfig);
}
