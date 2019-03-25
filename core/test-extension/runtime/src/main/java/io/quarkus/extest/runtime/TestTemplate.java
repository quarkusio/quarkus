package io.quarkus.extest.runtime;

import java.security.interfaces.DSAPublicKey;
import java.util.Set;

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
     * @param beanContainer - CDI container
     * @param beanClass - IConfigConsumer
     * @param buildTimeConfig - the extension TestBuildAndRunTimeConfig
     * @param runTimeConfig - the extension TestRunTimeConfig
     * @see IConfigConsumer#loadConfig(TestBuildAndRunTimeConfig, TestRunTimeConfig)
     */
    public void configureBeans(BeanContainer beanContainer, Class<IConfigConsumer> beanClass,
            TestBuildAndRunTimeConfig buildTimeConfig,
            TestRunTimeConfig runTimeConfig) {
        log.infof("Begin BeanContainerListener callback\n");
        IConfigConsumer instance = beanContainer.instance(beanClass);
        instance.loadConfig(buildTimeConfig, runTimeConfig);
        log.infof("configureBeans, instance=%s\n", instance);
    }

    public void loadDSAPublicKeyProducer(DSAPublicKey publicKey, BeanContainer value) {
        publicKey.getAlgorithm();
    }

    /**
     * Access the primitive class types at runtime to validate the build step generated deploy method
     * 
     * @param typesSet
     */
    public void validateTypes(Set<Class<?>> typesSet) {
        for (Class<?> type : typesSet) {
            log.infof("Checking type: %s", type.getName());
        }
    }
}
