package io.quarkus.extest.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.util.Collection;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.extest.runtime.IConfigConsumer;
import io.quarkus.extest.runtime.ObjectOfValue;
import io.quarkus.extest.runtime.ObjectValueOf;
import io.quarkus.extest.runtime.TestAnnotation;
import io.quarkus.extest.runtime.TestBuildAndRunTimeConfig;
import io.quarkus.extest.runtime.TestBuildTimeConfig;
import io.quarkus.extest.runtime.TestRunTimeConfig;
import io.quarkus.extest.runtime.TestTemplate;
import io.quarkus.runtime.RuntimeValue;

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
     * 
     * @return test-extension feature build item
     */
    @BuildStep(providesCapabilities = "io.quarkus.test-extension")
    FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem("test-extension");
    }

    /**
     * Register a custom bean defining annotation
     * 
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
        if (!buildTimeConfig.btSBV.getValue().equals("StringBasedValue")) {
            throw new IllegalStateException("buildTimeConfig.btSBV != StringBasedValue; " + buildTimeConfig.btSBV.getValue());
        }
        if (!buildTimeConfig.btSBVWithDefault.getValue().equals("btSBVWithDefaultValue")) {
            throw new IllegalStateException("buildTimeConfig.btSBVWithDefault != btSBVWithDefaultValue; "
                    + buildTimeConfig.btSBVWithDefault.getValue());
        }
        if (!buildTimeConfig.btStringOpt.equals("btStringOptValue")) {
            throw new IllegalStateException("buildTimeConfig.btStringOpt != btStringOptValue; " + buildTimeConfig.btStringOpt);
        }
        if (!buildTimeConfig.btStringOptWithDefault.equals("btStringOptWithDefaultValue")) {
            throw new IllegalStateException("buildTimeConfig.btStringOptWithDefault != btStringOptWithDefaultValue; "
                    + buildTimeConfig.btStringOptWithDefault);
        }
        if (!buildTimeConfig.allValues.oov.equals(new ObjectOfValue("configPart1", "configPart2"))) {
            throw new IllegalStateException("buildTimeConfig.oov != configPart1+onfigPart2; " + buildTimeConfig.allValues.oov);
        }
        if (!buildTimeConfig.allValues.oovWithDefault.equals(new ObjectOfValue("defaultPart1", "defaultPart2"))) {
            throw new IllegalStateException(
                    "buildTimeConfig.oovWithDefault != defaultPart1+defaultPart2; " + buildTimeConfig.allValues.oovWithDefault);
        }
        if (!buildTimeConfig.allValues.ovo.equals(new ObjectValueOf("configPart1", "configPart2"))) {
            throw new IllegalStateException("buildTimeConfig.oov != configPart1+onfigPart2; " + buildTimeConfig.allValues.oov);
        }
        if (!buildTimeConfig.allValues.ovoWithDefault.equals(new ObjectValueOf("defaultPart1", "defaultPart2"))) {
            throw new IllegalStateException(
                    "buildTimeConfig.oovWithDefault != defaultPart1+defaultPart2; " + buildTimeConfig.allValues.oovWithDefault);
        }
        if (buildTimeConfig.allValues.longPrimitive != 1234567891L) {
            throw new IllegalStateException(
                    "buildTimeConfig.allValues.longPrimitive != 1234567891L; " + buildTimeConfig.allValues.longPrimitive);
        }
        // quarkus.bt.all-values.double-primitive=3.1415926535897932384
        if (Math.IEEEremainder(buildTimeConfig.allValues.doublePrimitive, 3.1415926535897932384) != 0) {
            throw new IllegalStateException("buildTimeConfig.allValues.doublePrimitive != 3.1415926535897932384; "
                    + buildTimeConfig.allValues.doublePrimitive);
        }
        // quarkus.bt.all-values.opt-double-value=3.1415926535897932384
        if (Math.IEEEremainder(buildTimeConfig.allValues.optDoubleValue.getAsDouble(), 3.1415926535897932384) != 0) {
            throw new IllegalStateException("buildTimeConfig.allValues.optDoubleValue != 3.1415926535897932384; "
                    + buildTimeConfig.allValues.optDoubleValue);
        }
        if (buildTimeConfig.allValues.longValue != 1234567892L) {
            throw new IllegalStateException(
                    "buildTimeConfig.allValues.longValue != 1234567892L; " + buildTimeConfig.allValues.longValue);
        }
        if (buildTimeConfig.allValues.optLongValue.getAsLong() != 1234567893L) {
            throw new IllegalStateException(
                    "buildTimeConfig.optLongValue != 1234567893L; " + buildTimeConfig.allValues.optLongValue.getAsLong());
        }
        if (buildTimeConfig.allValues.optionalLongValue.get() != 1234567894L) {
            throw new IllegalStateException("buildTimeConfig.allValues.optionalLongValue != 1234567894L; "
                    + buildTimeConfig.allValues.optionalLongValue.get());
        }
        if (buildTimeConfig.allValues.nestedConfigMap.size() != 2) {
            throw new IllegalStateException(
                    "buildTimeConfig.allValues.simpleMap.size != 2; " + buildTimeConfig.allValues.nestedConfigMap.size());
        }
        //quarkus.bt.all-values.string-list=value1,value2
        if (buildTimeConfig.allValues.stringList.size() != 2) {
            throw new IllegalStateException(
                    "buildTimeConfig.allValues.stringList.size != 2; " + buildTimeConfig.allValues.stringList.size());
        }
        if (!buildTimeConfig.allValues.stringList.get(0).equals("value1")) {
            throw new IllegalStateException(
                    "buildTimeConfig.allValues.stringList[0] != value1; " + buildTimeConfig.allValues.stringList.get(0));
        }
        if (!buildTimeConfig.allValues.stringList.get(1).equals("value2")) {
            throw new IllegalStateException(
                    "buildTimeConfig.allValues.stringList[1] != value2; " + buildTimeConfig.allValues.stringList.get(1));
        }
        // quarkus.rt.all-values.long-list=1,2,3
        if (buildTimeConfig.allValues.longList.size() != 3) {
            throw new IllegalStateException(
                    "buildTimeConfig.allValues.longList.size != 3; " + buildTimeConfig.allValues.longList.size());
        }
        if (buildTimeConfig.allValues.longList.get(0) != 1) {
            throw new IllegalStateException(
                    "buildTimeConfig.allValues.longList[0] != 1; " + buildTimeConfig.allValues.longList.get(0));
        }
        if (buildTimeConfig.allValues.longList.get(2) != 3) {
            throw new IllegalStateException(
                    "buildTimeConfig.allValues.longList[2] != 3; " + buildTimeConfig.allValues.longList.get(2));
        }
    }

    /**
     * Collect the beans with our custom bean defining annotation and configure them with the runtime config
     * 
     * @param template - runtime template
     * @param beanArchiveIndex - index of type information
     * @param testBeanProducer - producer for located Class<IConfigConsumer> bean types
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void scanForBeans(TestTemplate template, BeanArchiveIndexBuildItem beanArchiveIndex,
            BuildProducer<TestBeanBuildItem> testBeanProducer) {
        IndexView indexView = beanArchiveIndex.getIndex();
        Collection<AnnotationInstance> testBeans = indexView.getAnnotations(TEST_ANNOTATION);
        for (AnnotationInstance ann : testBeans) {
            ClassInfo beanClassInfo = ann.target().asClass();
            try {
                Class<IConfigConsumer> beanClass = (Class<IConfigConsumer>) Class.forName(beanClassInfo.name().toString());
                testBeanProducer.produce(new TestBeanBuildItem(beanClass));
                System.out.printf("Configured bean: %s\n", beanClass);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * For each IConfigConsumer type, have the runtime template create a bean and pass in the runtime related configs
     * 
     * @param template - runtime template
     * @param testBeans - types of IConfigConsumer found
     * @param beanContainer - bean container to create test bean in
     */
    @BuildStep
    @Record(RUNTIME_INIT)
    void configureBeans(TestTemplate template, List<TestBeanBuildItem> testBeans, BeanContainerBuildItem beanContainer) {
        for (TestBeanBuildItem testBeanBuildItem : testBeans) {
            Class<IConfigConsumer> beanClass = testBeanBuildItem.getConfigConsumer();
            template.configureBeans(beanContainer.getValue(), beanClass, buildAndRunTimeConfig, runTimeConfig);
        }
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    ServiceStartBuildItem boot(LaunchModeBuildItem launchMode) {
        log.infof("boot, launchMode=%s", launchMode.getLaunchMode());
        return new ServiceStartBuildItem("test-service");
    }
}
