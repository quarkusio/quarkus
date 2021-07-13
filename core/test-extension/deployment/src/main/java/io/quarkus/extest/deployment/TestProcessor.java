package io.quarkus.extest.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.Provider;
import java.security.Security;
import java.security.interfaces.DSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;

import javax.enterprise.context.ApplicationScoped;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalStaticInitConfigSourceProviderBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LogHandlerBuildItem;
import io.quarkus.deployment.builditem.ObjectSubstitutionBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedPackageBuildItem;
import io.quarkus.extest.runtime.AdditionalStaticInitConfigSourceProvider;
import io.quarkus.extest.runtime.FinalFieldReflectionObject;
import io.quarkus.extest.runtime.IConfigConsumer;
import io.quarkus.extest.runtime.RuntimeXmlConfigService;
import io.quarkus.extest.runtime.TestAnnotation;
import io.quarkus.extest.runtime.TestRecorder;
import io.quarkus.extest.runtime.beans.CommandServlet;
import io.quarkus.extest.runtime.beans.PublicKeyProducer;
import io.quarkus.extest.runtime.config.AnotherPrefixConfig;
import io.quarkus.extest.runtime.config.FooRuntimeConfig;
import io.quarkus.extest.runtime.config.ObjectOfValue;
import io.quarkus.extest.runtime.config.ObjectValueOf;
import io.quarkus.extest.runtime.config.PrefixConfig;
import io.quarkus.extest.runtime.config.TestBuildAndRunTimeConfig;
import io.quarkus.extest.runtime.config.TestBuildTimeConfig;
import io.quarkus.extest.runtime.config.TestConfigRoot;
import io.quarkus.extest.runtime.config.TestRunTimeConfig;
import io.quarkus.extest.runtime.config.XmlConfig;
import io.quarkus.extest.runtime.config.named.PrefixNamedConfig;
import io.quarkus.extest.runtime.logging.AdditionalLogHandlerValueFactory;
import io.quarkus.extest.runtime.runtimeinitializedpackage.RuntimeInitializedClass;
import io.quarkus.extest.runtime.subst.DSAPublicKeyObjectSubstitution;
import io.quarkus.extest.runtime.subst.KeyProxy;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.undertow.deployment.ServletBuildItem;

/**
 * A test extension deployment processor
 */
public final class TestProcessor {
    static final Logger log = Logger.getLogger(TestProcessor.class);
    static DotName TEST_ANNOTATION = DotName.createSimple(TestAnnotation.class.getName());
    static DotName TEST_ANNOTATION_SCOPE = DotName.createSimple(ApplicationScoped.class.getName());

    TestConfigRoot configRoot;
    TestBuildTimeConfig buildTimeConfig;
    TestBuildAndRunTimeConfig buildAndRunTimeConfig;

    /**
     * Register an extension capability and feature
     *
     * @return test-extension feature build item
     */
    @BuildStep
    FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem("test-extension");
    }

    /**
     * Register a custom bean defining annotation
     *
     * @return BeanDefiningAnnotationBuildItem
     */
    @BuildStep
    BeanDefiningAnnotationBuildItem registerBeanDefiningAnnotations() {
        return new BeanDefiningAnnotationBuildItem(TEST_ANNOTATION, TEST_ANNOTATION_SCOPE);
    }

    /**
     * Register an additional log handler
     *
     * @return LogHandlerBuildItem
     */
    @BuildStep
    @Record(RUNTIME_INIT)
    LogHandlerBuildItem registerAdditionalLogHandler(final AdditionalLogHandlerValueFactory factory) {
        return new LogHandlerBuildItem(factory.create());
    }

    @BuildStep
    void registerNativeImageResources(BuildProducer<NativeImageResourceBuildItem> resource) {
        resource.produce(new NativeImageResourceBuildItem("/DSAPublicKey.encoded"));
    }

    /**
     * Register the CDI beans that are needed by the test extension
     *
     * @param additionalBeans - producer for additional bean items
     */
    @BuildStep
    void registerAdditionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        AdditionalBeanBuildItem additionalBeansItem = AdditionalBeanBuildItem.builder()
                .addBeanClass(PublicKeyProducer.class)
                .addBeanClass(CommandServlet.class)
                .setRemovable()
                .build();
        additionalBeans.produce(additionalBeansItem);
    }

    /**
     * Parse an XML configuration using JAXB into an XmlConfig instance graph
     *
     * @param recorder - runtime recorder
     * @return RuntimeServiceBuildItem
     * @throws JAXBException - on resource unmarshal failure
     */
    @BuildStep
    @Record(STATIC_INIT)
    RuntimeServiceBuildItem parseServiceXmlConfig(TestRecorder recorder) throws JAXBException, IOException {
        RuntimeServiceBuildItem serviceBuildItem = null;
        JAXBContext context = JAXBContext.newInstance(XmlConfig.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        try (InputStream is = getClass().getResourceAsStream("/config.xml")) {
            if (is != null) {
                log.info("Have XmlConfig, loading");
                XmlConfig config = (XmlConfig) unmarshaller.unmarshal(is);
                log.info("Loaded XmlConfig, creating service");
                RuntimeValue<RuntimeXmlConfigService> service = recorder.initRuntimeService(config);
                serviceBuildItem = new RuntimeServiceBuildItem(service);
            }
        }
        return serviceBuildItem;
    }

    /**
     * Have the runtime recorder start the service and install a shutdown hook
     *
     * @param recorder - runtime recorder
     * @param shutdownContextBuildItem - ShutdownContext information
     * @param serviceBuildItem - previously created RuntimeXmlConfigService container
     * @return ServiceStartBuildItem - build item indicating the RuntimeXmlConfigService startup
     * @throws IOException - on resource load failure
     */
    @BuildStep
    @Record(RUNTIME_INIT)
    ServiceStartBuildItem startRuntimeService(TestRecorder recorder, ShutdownContextBuildItem shutdownContextBuildItem,
            RuntimeServiceBuildItem serviceBuildItem) throws IOException {
        if (serviceBuildItem != null) {
            log.info("Registering service start");
            recorder.startRuntimeService(shutdownContextBuildItem, serviceBuildItem.getService());
        } else {
            log.info("No RuntimeServiceBuildItem seen, check config.xml");
        }
        return new ServiceStartBuildItem("RuntimeXmlConfigService");
    }

    /**
     * Load a DSAPublicKey from a resource and create an instance of it
     *
     * @param recorder - runtime recorder
     * @return PublicKeyBuildItem for the DSAPublicKey
     * @throws IOException - on resource load failure
     * @throws GeneralSecurityException - on key creation failure
     */
    @BuildStep
    @Record(STATIC_INIT)
    PublicKeyBuildItem loadDSAPublicKey(TestRecorder recorder,
            BuildProducer<ObjectSubstitutionBuildItem> substitutions) throws IOException, GeneralSecurityException {
        String path = configRoot.dsaKeyLocation;
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Failed to load resource: " + path);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String base64 = reader.readLine();
            reader.close();
            byte[] encoded = Base64.getDecoder().decode(base64);
            KeyFactory keyFactory = KeyFactory.getInstance("DSA");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encoded);
            DSAPublicKey publicKey = (DSAPublicKey) keyFactory.generatePublic(publicKeySpec);
            // Register how to serialize DSAPublicKey
            ObjectSubstitutionBuildItem.Holder<DSAPublicKey, KeyProxy> holder = new ObjectSubstitutionBuildItem.Holder(
                    DSAPublicKey.class, KeyProxy.class, DSAPublicKeyObjectSubstitution.class);
            ObjectSubstitutionBuildItem keySub = new ObjectSubstitutionBuildItem(holder);
            substitutions.produce(keySub);
            log.info("loadDSAPublicKey run");
            return new PublicKeyBuildItem(publicKey);
        }
    }

    /**
     * Have the runtime register the public key with the public key producer bean
     *
     * @param recorder - runtime recorder
     * @param publicKey - previously loaded public key
     * @param beanContainer - BeanContainer build item
     */
    @BuildStep
    @Record(RUNTIME_INIT)
    void loadDSAPublicKeyProducer(TestRecorder recorder, PublicKeyBuildItem publicKey, BeanContainerBuildItem beanContainer) {
        recorder.loadDSAPublicKeyProducer(publicKey.getPublicKey(), beanContainer.getValue());
    }

    /**
     * Register a servlet used for interacting with the native image for testing
     *
     * @return ServletBuildItem
     */
    @BuildStep
    ServletBuildItem createServlet() {
        return ServletBuildItem.builder("commands", CommandServlet.class.getName())
                .addMapping("/commands/*")
                .build();
    }

    /**
     * Validate the expected BUILD_TIME configuration
     */
    @BuildStep
    void checkConfig() {
        if (!configRoot.validateBuildConfig) {
            return;
        }

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
            throw new IllegalStateException("buildTimeConfig.oov != configPart1+configPart2; " + buildTimeConfig.allValues.oov);
        }
        if (!buildTimeConfig.allValues.oovWithDefault.equals(new ObjectOfValue("defaultPart1", "defaultPart2"))) {
            throw new IllegalStateException(
                    "buildTimeConfig.oovWithDefault != defaultPart1+defaultPart2; " + buildTimeConfig.allValues.oovWithDefault);
        }
        if (!buildTimeConfig.allValues.ovo.equals(new ObjectValueOf("configPart1", "configPart2"))) {
            throw new IllegalStateException("buildTimeConfig.oov != configPart1+configPart2; " + buildTimeConfig.allValues.oov);
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
     * @param recorder - runtime recorder
     * @param beanArchiveIndex - index of type information
     * @param testBeanProducer - producer for located Class<IConfigConsumer> bean types
     */
    @BuildStep
    @Record(STATIC_INIT)
    void scanForBeans(TestRecorder recorder, BeanArchiveIndexBuildItem beanArchiveIndex,
            BuildProducer<TestBeanBuildItem> testBeanProducer) {
        IndexView indexView = beanArchiveIndex.getIndex();
        Collection<AnnotationInstance> testBeans = indexView.getAnnotations(TEST_ANNOTATION);
        for (AnnotationInstance ann : testBeans) {
            ClassInfo beanClassInfo = ann.target().asClass();
            try {
                boolean isConfigConsumer = beanClassInfo.interfaceNames()
                        .stream()
                        .anyMatch(dotName -> dotName.equals(DotName.createSimple(IConfigConsumer.class.getName())));
                if (isConfigConsumer) {
                    Class<IConfigConsumer> beanClass = (Class<IConfigConsumer>) Class.forName(beanClassInfo.name().toString(),
                            true, Thread.currentThread().getContextClassLoader());
                    testBeanProducer.produce(new TestBeanBuildItem(beanClass));
                    log.infof("The configured bean: %s", beanClass);
                }
            } catch (ClassNotFoundException e) {
                log.warn("Failed to load bean class", e);
            }
        }
    }

    /**
     * For each IConfigConsumer type, have the runtime recorder create a bean and pass in the runtime related configs
     *
     * @param recorder - runtime recorder
     * @param testBeans - types of IConfigConsumer found
     * @param beanContainer - bean container to create test bean in
     * @param runTimeConfig - The RUN_TIME config phase root config
     */
    @BuildStep
    @Record(RUNTIME_INIT)
    void configureBeans(TestRecorder recorder, List<TestBeanBuildItem> testBeans,
            BeanContainerBuildItem beanContainer,
            TestRunTimeConfig runTimeConfig, FooRuntimeConfig fooRuntimeConfig, PrefixConfig prefixConfig,
            PrefixNamedConfig prefixNamedConfig,
            AnotherPrefixConfig anotherPrefixConfig) {
        for (TestBeanBuildItem testBeanBuildItem : testBeans) {
            Class<IConfigConsumer> beanClass = testBeanBuildItem.getConfigConsumer();
            recorder.configureBeans(beanContainer.getValue(), beanClass, buildAndRunTimeConfig, runTimeConfig,
                    fooRuntimeConfig, prefixConfig, prefixNamedConfig, anotherPrefixConfig);
        }
    }

    /**
     * Test for https://github.com/quarkusio/quarkus/issues/1633
     *
     * @param recorder - runtime recorder
     */
    @BuildStep
    @Record(RUNTIME_INIT)
    void referencePrimitiveTypeClasses(TestRecorder recorder) {
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
        recorder.validateTypes(allPrimitiveTypes);
    }

    @BuildStep
    ServiceStartBuildItem boot(LaunchModeBuildItem launchMode) {
        log.infof("boot, launchMode=%s", launchMode.getLaunchMode());
        return new ServiceStartBuildItem("test-service");
    }

    @BuildStep
    void registerSUNProvider(BuildProducer<ReflectiveClassBuildItem> classes) {
        Provider provider = Security.getProvider("SUN");
        ArrayList<String> providerClasses = new ArrayList<>();
        providerClasses.add(provider.getClass().getName());
        Set<Provider.Service> services = provider.getServices();
        for (Provider.Service service : services) {
            String serviceClass = service.getClassName();
            providerClasses.add(serviceClass);
            // Need to pull in the key classes
            String supportedKeyClasses = service.getAttribute("SupportedKeyClasses");
            if (supportedKeyClasses != null) {
                String[] keyClasses = supportedKeyClasses.split("\\|");
                providerClasses.addAll(Arrays.asList(keyClasses));
            }
        }
        for (String className : providerClasses) {
            classes.produce(new ReflectiveClassBuildItem(true, true, className));
            log.debugf("Register SUN.provider class: %s", className);
        }
    }

    @BuildStep
    void registerFinalFieldReflectionObject(BuildProducer<ReflectiveClassBuildItem> classes) {
        ReflectiveClassBuildItem finalField = ReflectiveClassBuildItem
                .builder(FinalFieldReflectionObject.class.getName())
                .methods(true)
                .fields(true)
                .finalFieldsWritable(true)
                .build();
        classes.produce(finalField);
    }

    @BuildStep
    void checkMapMap(TestBuildAndRunTimeConfig btrt, TestBuildTimeConfig bt, BuildProducer<ReflectiveClassBuildItem> unused) {
        if (!Objects.equals("1234", btrt.mapMap.get("outer-key").get("inner-key"))) {
            throw new AssertionError("BTRT map map failed");
        }
        if (!Objects.equals("1234", bt.mapMap.get("outer-key").get("inner-key"))) {
            throw new AssertionError("BT map map failed");
        }
    }

    @BuildStep
    RuntimeInitializedPackageBuildItem runtimeInitializedPackage() {
        return new RuntimeInitializedPackageBuildItem(RuntimeInitializedClass.class.getPackage().getName());
    }

    @BuildStep
    void deprecatedStaticInitBuildItem(
            BuildProducer<AdditionalStaticInitConfigSourceProviderBuildItem> additionalStaticInitConfigSourceProviders) {
        additionalStaticInitConfigSourceProviders.produce(new AdditionalStaticInitConfigSourceProviderBuildItem(
                AdditionalStaticInitConfigSourceProvider.class.getName()));
    }

    @BuildStep(onlyIf = Never.class)
    void neverRunThisOne() {
        throw new IllegalStateException("Not supposed to run!");
    }

    public static final class Never implements BooleanSupplier {
        TestBuildTimeConfig config;

        public boolean getAsBoolean() {
            if (config == null) {
                throw new IllegalStateException("Expected config");
            }
            return false;
        }
    }
}
