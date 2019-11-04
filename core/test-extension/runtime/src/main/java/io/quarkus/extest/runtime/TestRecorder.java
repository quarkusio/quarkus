package io.quarkus.extest.runtime;

import java.io.IOException;
import java.security.interfaces.DSAPublicKey;
import java.util.Set;

import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.extest.runtime.beans.PublicKeyProducer;
import io.quarkus.extest.runtime.config.TestBuildAndRunTimeConfig;
import io.quarkus.extest.runtime.config.TestRunTimeConfig;
import io.quarkus.extest.runtime.config.XmlConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

/**
 * The runtime recorder
 *
 * note that the use of the deprecated @Template is deliberate, to ensure that it still works.
 *
 */
@SuppressWarnings("deprecation")
@Recorder
public class TestRecorder {
    static final Logger log = Logger.getLogger(TestRecorder.class);

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

    /**
     * Create a non-CDI based RuntimeXmlConfigService from the XmlConfig
     * 
     * @param config - parse XML configuration
     * @return RuntimeValue<RuntimeXmlConfigService>
     */
    public RuntimeValue<RuntimeXmlConfigService> initRuntimeService(XmlConfig config) {
        RuntimeXmlConfigService service = new RuntimeXmlConfigService(config);
        return new RuntimeValue<>(service);
    }

    /**
     * Invoke the RuntimeXmlConfigService#startService method and register a stopService call with the shutdown context.
     *
     * @param shutdownContext - context for adding shutdown hooks
     * @param runtimeValue - service value
     * @throws IOException - on startup failure
     */
    public void startRuntimeService(ShutdownContext shutdownContext, RuntimeValue<RuntimeXmlConfigService> runtimeValue)
            throws IOException {
        RuntimeXmlConfigService service = runtimeValue.getValue();
        service.startService();
        shutdownContext.addShutdownTask(service::stopService);
    }

    /**
     * Passes the public ket to the PublicKeyProducer for injection into CDI beans at runtime
     * 
     * @param publicKey - public key
     * @param beanContainer - CDI bean container
     */
    public void loadDSAPublicKeyProducer(DSAPublicKey publicKey, BeanContainer beanContainer) {
        PublicKeyProducer keyProducer = beanContainer.instance(PublicKeyProducer.class);
        keyProducer.setPublicKey(publicKey);
    }

    /**
     * Access the primitive class types at runtime to validate the build step generated deploy method
     * 
     * @param typesSet - primitive classes set
     */
    public void validateTypes(Set<Class<?>> typesSet) {
        for (Class<?> type : typesSet) {
            log.debugf("Checking type: %s", type.getName());
        }
    }
}
