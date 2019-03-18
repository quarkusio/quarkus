package io.quarkus.extest.runtime;

import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Template;

/**
 * The runtime template
 */
@Template
public class TestTemplate {
    static final Logger log = Logger.getLogger(TestTemplate.class);

    /**
     * Instantiate the given class in the given BeanContainer and passes the TestBuildAndRunTimeConfig and TestRunTimeConfig to
     * it
     * 
     * @see IConfigConsumer#loadConfig(TestBuildAndRunTimeConfig, TestRunTimeConfig)
     * @param beanContainer - CDI container
     * @param beanClass - IConfigConsumer
     * @param buildTimeConfig - the extension TestBuildAndRunTimeConfig
     * @param runTimeConfig - the extension TestRunTimeConfig
     */
    public void configureBeans(BeanContainer beanContainer, Class<IConfigConsumer> beanClass,
            TestBuildAndRunTimeConfig buildTimeConfig,
            TestRunTimeConfig runTimeConfig) {
        log.infof("Begin BeanContainerListener callback\n");
        IConfigConsumer instance = beanContainer.instance(beanClass);
        instance.loadConfig(buildTimeConfig, runTimeConfig);
        log.infof("configureBeans, instance=%s\n", instance);
    }
}
