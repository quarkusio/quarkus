package org.jboss.shamrock.camel.deployment;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.xml.bind.annotation.XmlAccessOrder;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttachmentRef;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlInlineBinaryData;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSchemaTypes;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;

import org.apache.camel.Consumer;
import org.apache.camel.Converter;
import org.apache.camel.Endpoint;
import org.apache.camel.Producer;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.TypeConverter;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.file.GenericFileProcessStrategy;
import org.apache.camel.component.file.strategy.GenericFileProcessStrategyFactory;
import org.apache.camel.impl.converter.DoubleMap;
import org.apache.camel.spi.ExchangeFormatter;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.annotations.Record;
import org.jboss.shamrock.camel.runtime.CamelRuntime;
import org.jboss.shamrock.camel.runtime.CamelTemplate;
import org.jboss.shamrock.camel.runtime.RuntimeRegistry;
import org.jboss.shamrock.deployment.buildconfig.BuildConfig;
import org.jboss.shamrock.deployment.buildconfig.BuildConfig.ConfigNode;
import org.jboss.shamrock.deployment.builditem.ApplicationArchivesBuildItem;
import org.jboss.shamrock.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.shamrock.deployment.builditem.ShutdownContextBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveMethodBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateConfigBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateResourceBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateResourceBundleBuildItem;
import org.jboss.shamrock.deployment.recording.RecorderContext;
import org.jboss.shamrock.runtime.RuntimeValue;



import static org.jboss.shamrock.annotations.ExecutionTime.RUNTIME_INIT;
import static org.jboss.shamrock.annotations.ExecutionTime.STATIC_INIT;

class CamelProcessor {

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    @Inject
    BuildProducer<ReflectiveMethodBuildItem> reflectiveMethod;

    @Inject
    BuildProducer<SubstrateResourceBuildItem> resource;

    @Inject
    BuildProducer<SubstrateResourceBundleBuildItem> resourceBundle;

    @Inject
    ApplicationArchivesBuildItem applicationArchivesBuildItem;

    @Inject
    CombinedIndexBuildItem combinedIndexBuildItem;

    @BuildStep
    SubstrateConfigBuildItem processSystemProperties() {
        return SubstrateConfigBuildItem.builder()
                .addNativeImageSystemProperty("CamelSimpleLRUCacheFactory", "true")
                .build();
    }

    @BuildStep
    void processJaxb() {
        addReflectiveClass(false, false, "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
        addReflectiveClass(false, false, "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
        addReflectiveClass(false, false, "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");
        addReflectiveClass(true, false, "com.sun.xml.bind.v2.ContextFactory");
        addReflectiveClass(true, false, "com.sun.xml.internal.bind.v2.ContextFactory");
        Stream.of(XmlAccessOrder.class, XmlAccessorType.class, XmlAnyAttribute.class, XmlAnyElement.class, XmlAttachmentRef.class, XmlAttribute.class,
                XmlElement.class, XmlElementDecl.class, XmlElementRef.class, XmlElementRefs.class, XmlElements.class, XmlElementWrapper.class,
                XmlEnum.class, XmlEnumValue.class, XmlID.class, XmlIDREF.class, XmlInlineBinaryData.class, XmlList.class, XmlMimeType.class,
                XmlMixed.class, XmlNs.class, XmlRegistry.class, XmlRootElement.class, XmlSchema.class, XmlSchemaType.class, XmlSchemaTypes.class,
                XmlSeeAlso.class, XmlTransient.class, XmlType.class, XmlValue.class,
                XmlJavaTypeAdapter.class, XmlJavaTypeAdapters.class)
                .map(Class::getName)
                .forEach(clazz -> addReflectiveClass(true, false, clazz));
        addResourceBundle("javax.xml.bind.Messages");
        addResourceBundle("com.sun.org.apache.xml.internal.serializer.utils.SerializerMessages");
        addResourceBundle("com.sun.org.apache.xml.internal.res.XMLErrorResources");
        Stream.of("html", "text", "xml", "unknown")
                .map(s -> "com/sun/org/apache/xml/internal/serializer/output_" + s + ".properties")
                .forEach(this::addResource);
        iterateResources("org/apache/camel")
                .filter(p -> p.getFileName().toString().equals("jaxb.index"))
                .forEach(this::handleJaxbFile);
    }

    @BuildStep
    void processCamelReflectiveClasses() {
        // Public implementations of Camel interfaces
        Stream.of(Endpoint.class, Consumer.class, Producer.class, TypeConverter.class,
                ExchangeFormatter.class, GenericFileProcessStrategy.class)
                .map(Class::getName)
                .map(DotName::createSimple)
                .map(combinedIndexBuildItem.getIndex()::getAllKnownImplementors)
                .flatMap(Collection::stream)
                .filter(this::isPublic)
                .forEach(v -> addReflectiveClass(true, true, v.name().toString()));
        addReflectiveClass(false, false, GenericFile.class.getName());
        // GenericFileProcessStrategyFactory is used through reflection
        addReflectiveClass(true, false, GenericFileProcessStrategyFactory.class.getName());
    }

    @BuildStep
    void processConverters() {
        combinedIndexBuildItem.getIndex().getAnnotations(DotName.createSimple(Converter.class.getName()))
                .forEach(v -> {
                            if (v.target().kind() == AnnotationTarget.Kind.CLASS) {
                                addReflectiveClass(true, false, v.target().asClass().name().toString());
                            }
                            if (v.target().kind() == AnnotationTarget.Kind.METHOD) {
                                addReflectiveMethod(v.target().asMethod());
                            }
                        }
                );
    }

    @BuildStep
    public void process() throws Exception {
        processServices();
    }

    @BuildStep
    @Record(STATIC_INIT)
    protected CamelRuntimeBuildItem createInitTask(BuildConfig buildConfig,
                                                   RecorderContext recorderContext,
                                                   CamelTemplate template) throws Exception {
        Properties properties = new Properties();
        ConfigNode config = buildConfig.getApplicationConfig();
        storeProperties(properties, config, "");

        String clazz = properties.getProperty(CamelRuntime.PROP_CAMEL_RUNTIME, CamelRuntime.class.getName());
        RuntimeValue<?> iruntime = recorderContext.newInstance(clazz);

        RuntimeRegistry registry = new RuntimeRegistry();
        processServices().forEach((n, c, o) -> registry.bind(n, c, recorderContext.newInstance(o)));

        List<RuntimeValue<?>> ibuilders = getInitRouteBuilderClasses()
                .map(recorderContext::newInstance).collect(Collectors.toList());

        return new CamelRuntimeBuildItem(template.init(iruntime, registry, properties, ibuilders));
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    void createDeploymentTask(CamelTemplate template, CamelRuntimeBuildItem runtime, ShutdownContextBuildItem shutdown) throws Exception {
        template.start(shutdown, runtime.getRuntime());
    }

    protected Stream<String> getInitRouteBuilderClasses() {
        return combinedIndexBuildItem.getIndex().getAllKnownImplementors(DotName.createSimple(RoutesBuilder.class.getName()))
                .stream()
                .filter(this::isConcrete)
                .filter(this::isPublic)
                .map(ClassInfo::toString);
    }

    // Camel services files
    protected DoubleMap<String, Class<?>, String> processServices() {
        DoubleMap<String, Class<?>, String> map = new DoubleMap<>(256);
        iterateResources("META-INF/services/org/apache/camel")
                .forEach(p -> addCamelService(p, map));
        return map;
    }

    protected void addCamelService(Path p, DoubleMap<String, Class<?>, String> map) {
        String name = p.getFileName().toString();
        try (InputStream is = Files.newInputStream(p)) {
            Properties props = new Properties();
            props.load(is);
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                String k = entry.getKey().toString();
                if (k.equals("class")) {
                    String clazz = entry.getValue().toString();
                    Class cl = Class.forName(clazz);
                    map.put(name, cl, clazz);
                    addReflectiveClass(true, false, clazz);
                } else if (k.endsWith(".class")) {
                    // Used for strategy.factory.class
                    String clazz = entry.getValue().toString();
                    addReflectiveClass(true, false, clazz);
                    addResource(p);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected boolean isConcrete(ClassInfo ci) {
        return (ci.flags() & Modifier.ABSTRACT) == 0;
    }

    protected boolean isPublic(ClassInfo ci) {
        return (ci.flags() & Modifier.PUBLIC) != 0;
    }

    protected Stream<Path> iterateResources(String path) {
        return applicationArchivesBuildItem.getAllApplicationArchives().stream()
                .map(arch -> arch.getArchiveRoot().resolve(path))
                .filter(Files::isDirectory)
                .flatMap(this::safeWalk)
                .filter(Files::isRegularFile);
    }

    protected void addResource(Path p) {
        addResource(p.toString().substring(1));
    }

    protected void addResource(String r) {
        resource.produce(new SubstrateResourceBuildItem(r));
    }

    protected void addReflectiveClass(boolean methods, boolean fields, String... className) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(methods, false, className));
    }

    protected void addReflectiveMethod(MethodInfo mi) {
        reflectiveMethod.produce(new ReflectiveMethodBuildItem(mi));
    }

    protected void addResourceBundle(String bundle) {
        resourceBundle.produce(new SubstrateResourceBundleBuildItem(bundle));
    }

    protected void handleJaxbFile(Path p) {
        try {
            String path = p.toAbsolutePath().toString().substring(1);
            String pkg = p.toAbsolutePath().getParent().toString().substring(1).replace("/", ".") + ".";
            addResource(path);
            for (String line : Files.readAllLines(p)) {
                if (!line.startsWith("#")) {
                    String clazz = pkg + line.trim();
                    Class cl = Class.forName(clazz);
                    while (cl != Object.class) {
                        addReflectiveClass(true, true, cl.getName());
                        cl = cl.getSuperclass();
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void storeProperties(Properties properties, ConfigNode config, String prefix) {
        String s = config.asString();
        if (s != null && !prefix.isEmpty()) {
            properties.setProperty(prefix, s);
        }
        for (String key : config.getChildKeys()) {
            storeProperties(properties, config.get(key), prefix.isEmpty() ? key : prefix + "." + key);
        }
    }

    protected Stream<Path> safeWalk(Path p) {
        try {
            return Files.walk(p);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

}
