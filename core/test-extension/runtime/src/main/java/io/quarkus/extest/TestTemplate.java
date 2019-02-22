package io.quarkus.extest;

import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.annotations.Template;

/**
 * The runtime template
 */
@Template
public class TestTemplate {
    static final Logger log = Logger.getLogger(TestTemplate.class);

    /**
     * Create a BeanContainerListener that instantiates the given class and passes the TestRunTimeConfig to it
     * 
     * @see IConfigConsumer#loadConfig(TestBuildAndRunTimeConfig, TestRunTimeConfig)
     * @param beanClass - IConfigConsumer
     * @param runTimeConfig - the extension TestRunTimeConfig
     * @return BeanContainerListener
     */
    public BeanContainerListener configureBeans(Class<IConfigConsumer> beanClass, TestBuildAndRunTimeConfig buildTimeConfig,
            TestRunTimeConfig runTimeConfig) {
        return new BeanContainerListener() {
            @Override
            public void created(BeanContainer beanContainer) {
                log.infof("Begin BeanContainerListener callback\n");
                IConfigConsumer instance = beanContainer.instance(beanClass);
                instance.loadConfig(buildTimeConfig, runTimeConfig);
                log.infof("configureBeans, instance=%s\n", instance);
            }
        };
    }
}
