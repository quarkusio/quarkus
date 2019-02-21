package org.jboss.shamrock.extest;

import java.util.Collection;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;
import org.jboss.shamrock.arc.deployment.BeanArchiveIndexBuildItem;
import org.jboss.shamrock.arc.deployment.BeanContainerListenerBuildItem;
import org.jboss.shamrock.arc.deployment.BeanDefiningAnnotationBuildItem;
import org.jboss.shamrock.arc.runtime.BeanContainerListener;
import org.jboss.shamrock.deployment.annotations.BuildProducer;
import org.jboss.shamrock.deployment.annotations.BuildStep;
import org.jboss.shamrock.deployment.annotations.ExecutionTime;
import org.jboss.shamrock.deployment.annotations.Record;
import org.jboss.shamrock.deployment.builditem.FeatureBuildItem;
import org.jboss.shamrock.deployment.builditem.LaunchModeBuildItem;
import org.jboss.shamrock.deployment.builditem.ServiceStartBuildItem;

import static org.jboss.shamrock.deployment.annotations.ExecutionTime.RUNTIME_INIT;

/**
 * A test extension deployment processor
 */
public final class TestProcessor {
    static final Logger log = Logger.getLogger(TestProcessor.class);
    static DotName TEST_ANNOTATION = DotName.createSimple(TestAnnotation.class.getName());

    TestBuildTimeConfig buildTimeConfig;
    TestBuildAndRunTimeConfig buildAndRunTimeConfig;
    TestRunTimeConfig runTimeConfig;

    /**
     * Register a extension capability and feature
     * @return test-extension feature build item
     */
    @BuildStep(providesCapabilities = "org.jboss.shamrock.test-extension")
    FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem("test-extension");
    }

    /**
     * Register a custom bean defining annotation
     * @return
     */
    @BuildStep
    BeanDefiningAnnotationBuildItem registerX() {
        return new BeanDefiningAnnotationBuildItem(TEST_ANNOTATION);
    }

    /**
     * Validate the expected BUILD_TIME configuration
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void checkConfig() {
        // Deployment time configuration
        if(!buildTimeConfig.btSBV.getValue().equals("StringBasedValue")) {
            throw new IllegalStateException("buildTimeConfig.btSBV != StringBasedValue; "+buildTimeConfig.btSBV.getValue());
        }
        if(!buildTimeConfig.btSBVWithDefault.getValue().equals("btSBVWithDefaultValue")) {
            throw new IllegalStateException("buildTimeConfig.btSBVWithDefault != btSBVWithDefaultValue; "+buildTimeConfig.btSBVWithDefault.getValue());
        }
        if(!buildTimeConfig.btStringOpt.equals("btStringOptValue")) {
            throw new IllegalStateException("buildTimeConfig.btStringOpt != btStringOptValue; "+buildTimeConfig.btStringOpt);
        }
        if(!buildTimeConfig.btStringOptWithDefault.equals("btStringOptWithDefaultValue")) {
            throw new IllegalStateException("buildTimeConfig.btStringOptWithDefault != btStringOptWithDefaultValue; "+buildTimeConfig.btStringOptWithDefault);
        }
        if(!buildTimeConfig.allValues.oov.equals(new ObjectOfValue("configPart1", "configPart2"))) {
            throw new IllegalStateException("buildTimeConfig.oov != configPart1+onfigPart2; "+buildTimeConfig.allValues.oov);
        }
        if(!buildTimeConfig.allValues.oovWithDefault.equals(new ObjectOfValue("defaultPart1", "defaultPart2"))) {
            throw new IllegalStateException("buildTimeConfig.oovWithDefault != defaultPart1+defaultPart2; "+buildTimeConfig.allValues.oovWithDefault);
        }
        if(!buildTimeConfig.allValues.ovo.equals(new ObjectValueOf("configPart1", "configPart2"))) {
            throw new IllegalStateException("buildTimeConfig.oov != configPart1+onfigPart2; "+buildTimeConfig.allValues.oov);
        }
        if(!buildTimeConfig.allValues.ovoWithDefault.equals(new ObjectValueOf("defaultPart1", "defaultPart2"))) {
            throw new IllegalStateException("buildTimeConfig.oovWithDefault != defaultPart1+defaultPart2; "+buildTimeConfig.allValues.oovWithDefault);
        }
        if(buildTimeConfig.allValues.longPrimitive != 1234567891L) {
            throw new IllegalStateException("buildTimeConfig.allValues.longPrimitive != 1234567891L; "+buildTimeConfig.allValues.longPrimitive);
        }
        if(buildTimeConfig.allValues.longValue != 1234567892L) {
            throw new IllegalStateException("buildTimeConfig.allValues.longValue != 1234567892L; "+buildTimeConfig.allValues.longValue);
        }
        if(buildTimeConfig.allValues.optLongValue.getAsLong() != 1234567893L) {
            throw new IllegalStateException("buildTimeConfig.optLongValue != 1234567893L; "+buildTimeConfig.allValues.optLongValue.getAsLong());
        }
        if(buildTimeConfig.allValues.optionalLongValue.get() != 1234567894L) {
            throw new IllegalStateException("buildTimeConfig.allValues.optionalLongValue != 1234567894L; "+buildTimeConfig.allValues.optionalLongValue.get());
        }
        if(buildTimeConfig.allValues.nestedConfigMap.size() != 2) {
            throw new IllegalStateException("buildTimeConfig.allValues.simpleMap.size != 2; "+buildTimeConfig.allValues.nestedConfigMap.size());
        }
    }

    /**
     * Collect the beans with our custom bean defining annotation and configure them with the runtime config
     * @param template
     * @param beanArchiveIndex
     * @param listeners
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void configureBean(TestTemplate template, BeanArchiveIndexBuildItem beanArchiveIndex, BuildProducer<BeanContainerListenerBuildItem> listeners) {
        IndexView indexView = beanArchiveIndex.getIndex();
        Collection<AnnotationInstance> testBeans = indexView.getAnnotations(TEST_ANNOTATION);
        for(AnnotationInstance ann : testBeans) {
            ClassInfo beanClassInfo = ann.target().asClass();
            try {
                Class<IConfigConsumer> beanClass = (Class<IConfigConsumer>) Class.forName(beanClassInfo.name().toString());
                BeanContainerListener listener = template.configureBeans(beanClass, buildAndRunTimeConfig, runTimeConfig);
                listeners.produce(new BeanContainerListenerBuildItem(listener));
                System.out.printf("Configured bean: %s\n", beanClass);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    ServiceStartBuildItem boot(LaunchModeBuildItem launchMode) {
        log.infof("boot, launchMode=%s", launchMode.getLaunchMode());
        return new ServiceStartBuildItem("test-service");
    }
}
