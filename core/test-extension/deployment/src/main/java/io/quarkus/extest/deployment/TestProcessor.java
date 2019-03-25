package io.quarkus.extest.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.DSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ObjectSubstitutionBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.extest.runtime.IConfigConsumer;
import io.quarkus.extest.runtime.ObjectOfValue;
import io.quarkus.extest.runtime.ObjectValueOf;
import io.quarkus.extest.runtime.TestAnnotation;
import io.quarkus.extest.runtime.TestBuildAndRunTimeConfig;
import io.quarkus.extest.runtime.TestBuildTimeConfig;
import io.quarkus.extest.runtime.TestRunTimeConfig;
import io.quarkus.extest.runtime.TestTemplate;
import io.quarkus.extest.runtime.subst.DSAPublicKeyObjectSubstitution;
import io.quarkus.extest.runtime.subst.KeyProxy;

/**
 * A test extension deployment processor
 */
public final class TestProcessor {
    static final Logger log = Logger.getLogger(TestProcessor.class);
    static DotName TEST_ANNOTATION = DotName.createSimple(TestAnnotation.class.getName());

    TestBuildTimeConfig buildTimeConfig;
    TestBuildAndRunTimeConfig buildAndRunTimeConfig;

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
    @Record(STATIC_INIT)
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

    @BuildStep
    @Record(STATIC_INIT)
    PublicKeyBuildItem loadDSAPublicKey(TestTemplate template,
            BuildProducer<ObjectSubstitutionBuildItem> substitutions) throws GeneralSecurityException {
        String base64 = "MIIDQjCCAjUGByqGSM44BAEwggIoAoIBAQCPeTXZuarpv6vtiHrPSVG28y7FnjuvNxjo6sSWHz79NgbnQ1GpxBgzObg" +
                "J58KuHFObp0dbhdARrbi0eYd1SYRpXKwOjxSzNggooi/6JxEKPWKpk0U0CaD+aWxGWPhL3SCBnDcJoBBXsZWtzQAjPbpUhLYpH51k" +
                "jviDRIZ3l5zsBLQ0pqwudemYXeI9sCkvwRGMn/qdgYHnM423krcw17njSVkvaAmYchU5Feo9a4tGU8YzRY+AOzKkwuDycpAlbk4/i" +
                "jsIOKHEUOThjBopo33fXqFD3ktm/wSQPtXPFiPhWNSHxgjpfyEc2B3KI8tuOAdl+CLjQr5ITAV2OTlgHNZnAh0AuvaWpoV499/e5/" +
                "pnyXfHhe8ysjO65YDAvNVpXQKCAQAWplxYIEhQcE51AqOXVwQNNNo6NHjBVNTkpcAtJC7gT5bmHkvQkEq9rI837rHgnzGC0jyQQ8" +
                "tkL4gAQWDt+coJsyB2p5wypifyRz6Rh5uixOdEvSCBVEy1W4AsNo0fqD7UielOD6BojjJCilx4xHjGjQUntxyaOrsLC+EsRGiWOef" +
                "TznTbEBplqiuH9kxoJts+xy9LVZmDS7TtsC98kOmkltOlXVNb6/xF1PYZ9j897buHOSXC8iTgdzEpbaiH7B5HSPh++1/et1SEMWs" +
                "iMt7lU92vAhErDR8C2jCXMiT+J67ai51LKSLZuovjntnhA6Y8UoELxoi34u1DFuHvF9veA4IBBQACggEAK6IeZShhydDUM5XsOJ/V" +
                "AYPOgrnLr30AfKWLR39+FJBunVMWNPpvO5D9dU7B6nmSiLATpwhBDNEhyJ0ltmBGuFDBAkKkqE4l6l2iVh+C1TyYliv1P2LCJFNgr" +
                "AJxyr+5Q5zM9hUgfbT66xnwCf/4aiO7nBlj4wOL3l9ABVllYifMZyKVYFGluXmo+jyyeAcCtzHi5SABbTOQJN0WXTlGtzxLFQ0QErD" +
                "GhP1/A6z5lw5VHJn2aWMeTCaH+rJZpQfM8b2VWr7UEljqFgpSIHbrImuXcf2nP6uZLKFiDdAjDUyj0h2jXwwcdhwWXuhOEv8XIilkc" +
                "9nMcPLqbdcQ4M5agg==";
        byte[] encoded = Base64.getDecoder().decode(base64);
        KeyFactory keyFactory = KeyFactory.getInstance("DSA");
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encoded);
        DSAPublicKey publicKey = (DSAPublicKey) keyFactory.generatePublic(publicKeySpec);
        ObjectSubstitutionBuildItem.Holder<DSAPublicKey, KeyProxy> holder = new ObjectSubstitutionBuildItem.Holder(
                DSAPublicKey.class, KeyProxy.class, DSAPublicKeyObjectSubstitution.class);
        ObjectSubstitutionBuildItem keysub = new ObjectSubstitutionBuildItem(holder);
        substitutions.produce(keysub);
        log.infof("loadDSAPublicKey run");
        return new PublicKeyBuildItem(publicKey);
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    void loadDSAPublicKeyProducer(TestTemplate template, PublicKeyBuildItem publicKey, BeanContainerBuildItem beanContainer) {
        template.loadDSAPublicKeyProducer(publicKey.getPublicKey(), beanContainer.getValue());
    }

    /**
     * Collect the beans with our custom bean defining annotation and configure them with the runtime config
     *
     * @param template - runtime template
     * @param beanArchiveIndex - index of type information
     * @param testBeanProducer - producer for located Class<IConfigConsumer> bean types
     */
    @BuildStep
    @Record(STATIC_INIT)
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
    void configureBeans(TestTemplate template, List<TestBeanBuildItem> testBeans, BeanContainerBuildItem beanContainer,
            TestRunTimeConfig runTimeConfig) {
        for (TestBeanBuildItem testBeanBuildItem : testBeans) {
            Class<IConfigConsumer> beanClass = testBeanBuildItem.getConfigConsumer();
            template.configureBeans(beanContainer.getValue(), beanClass, buildAndRunTimeConfig, runTimeConfig);
        }
    }

    /**
     * Test for https://github.com/quarkusio/quarkus/issues/1633
     * 
     * @param template - runtime template
     */
    @BuildStep
    @Record(RUNTIME_INIT)
    void referencePrimitiveTypeClasses(TestTemplate template) {
        HashSet<Class<?>> allPrimitiveTypes = new HashSet<>();
        allPrimitiveTypes.add(byte.class);
        allPrimitiveTypes.add(char.class);
        allPrimitiveTypes.add(short.class);
        allPrimitiveTypes.add(int.class);
        allPrimitiveTypes.add(long.class);
        allPrimitiveTypes.add(float.class);
        allPrimitiveTypes.add(double.class);
        allPrimitiveTypes.add(byte[].class);
        allPrimitiveTypes.add(char[].class);
        allPrimitiveTypes.add(short[].class);
        allPrimitiveTypes.add(int[].class);
        allPrimitiveTypes.add(long[].class);
        allPrimitiveTypes.add(float[].class);
        allPrimitiveTypes.add(double[].class);
        template.validateTypes(allPrimitiveTypes);
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    ServiceStartBuildItem boot(LaunchModeBuildItem launchMode) {
        log.infof("boot, launchMode=%s", launchMode.getLaunchMode());
        return new ServiceStartBuildItem("test-service");
    }
}