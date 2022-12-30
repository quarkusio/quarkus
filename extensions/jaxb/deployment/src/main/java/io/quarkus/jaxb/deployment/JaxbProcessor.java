package io.quarkus.jaxb.deployment;

import java.io.IOError;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.xml.bind.JAXBContext;
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

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyIgnoreWarningBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.jaxb.runtime.JaxbContextConfigRecorder;
import io.quarkus.jaxb.runtime.JaxbContextProducer;

class JaxbProcessor {

    private static final List<Class<? extends Annotation>> JAXB_ANNOTATIONS = List.of(
            XmlAccessorType.class,
            XmlAnyAttribute.class,
            XmlAnyElement.class,
            XmlAttachmentRef.class,
            XmlAttribute.class,
            XmlElement.class,
            XmlElementDecl.class,
            XmlElementRef.class,
            XmlElementRefs.class,
            XmlElements.class,
            XmlElementWrapper.class,
            XmlEnum.class,
            XmlEnumValue.class,
            XmlID.class,
            XmlIDREF.class,
            XmlInlineBinaryData.class,
            XmlList.class,
            XmlMimeType.class,
            XmlMixed.class,
            XmlNs.class,
            XmlRegistry.class,
            XmlRootElement.class,
            XmlSchema.class,
            XmlSchemaType.class,
            XmlSchemaTypes.class,
            XmlSeeAlso.class,
            XmlTransient.class,
            XmlType.class,
            XmlValue.class,
            XmlJavaTypeAdapter.class,
            XmlJavaTypeAdapters.class);

    private static final List<Class<?>> JAXB_REFLECTIVE_CLASSES = List.of(XmlAccessOrder.class);

    private static final DotName XML_ROOT_ELEMENT = DotName.createSimple(XmlRootElement.class.getName());
    private static final DotName XML_TYPE = DotName.createSimple(XmlType.class.getName());
    private static final DotName XML_REGISTRY = DotName.createSimple(XmlRegistry.class.getName());
    private static final DotName XML_SCHEMA = DotName.createSimple(XmlSchema.class.getName());
    private static final DotName XML_JAVA_TYPE_ADAPTER = DotName.createSimple(XmlJavaTypeAdapter.class.getName());
    private static final DotName XML_ANY_ELEMENT = DotName.createSimple(XmlAnyElement.class.getName());
    private static final DotName XML_SEE_ALSO = DotName.createSimple(XmlSeeAlso.class.getName());

    private static final List<DotName> JAXB_ROOT_ANNOTATIONS = List.of(XML_ROOT_ELEMENT, XML_TYPE, XML_REGISTRY);

    private static final List<DotName> IGNORE_TYPES = List.of(DotName.createSimple("javax.xml.datatype.XMLGregorianCalendar"));

    private static final List<String> NATIVE_PROXY_DEFINITIONS = List.of(
            "com.sun.xml.bind.marshaller.CharacterEscapeHandler",
            "com.sun.xml.txw2.output.CharacterEscapeHandler",
            "com.sun.xml.bind.v2.schemagen.episode.Bindings",
            "com.sun.xml.bind.v2.schemagen.episode.SchemaBindings",
            "com.sun.xml.bind.v2.schemagen.episode.Klass",
            "com.sun.xml.bind.v2.schemagen.episode.Package",
            "com.sun.xml.bind.v2.schemagen.xmlschema.Annotated",
            "com.sun.xml.bind.v2.schemagen.xmlschema.Annotation",
            "com.sun.xml.bind.v2.schemagen.xmlschema.Any",
            "com.sun.xml.bind.v2.schemagen.xmlschema.Appinfo",
            "com.sun.xml.bind.v2.schemagen.xmlschema.AttrDecls",
            "com.sun.xml.bind.v2.schemagen.xmlschema.AttributeType",
            "com.sun.xml.bind.v2.schemagen.xmlschema.ComplexContent",
            "com.sun.xml.bind.v2.schemagen.xmlschema.ComplexExtension",
            "com.sun.xml.bind.v2.schemagen.xmlschema.ComplexRestriction",
            "com.sun.xml.bind.v2.schemagen.xmlschema.ComplexType",
            "com.sun.xml.bind.v2.schemagen.xmlschema.ComplexTypeHost",
            "com.sun.xml.bind.v2.schemagen.xmlschema.ComplexTypeModel",
            "com.sun.xml.bind.v2.schemagen.xmlschema.ContentModelContainer",
            "com.sun.xml.bind.v2.schemagen.xmlschema.Documentation",
            "com.sun.xml.bind.v2.schemagen.xmlschema.Element",
            "com.sun.xml.bind.v2.schemagen.xmlschema.ExplicitGroup",
            "com.sun.xml.bind.v2.schemagen.xmlschema.ExtensionType",
            "com.sun.xml.bind.v2.schemagen.xmlschema.FixedOrDefault",
            "com.sun.xml.bind.v2.schemagen.xmlschema.Import",
            "com.sun.xml.bind.v2.schemagen.xmlschema.List",
            "com.sun.xml.bind.v2.schemagen.xmlschema.LocalAttribute",
            "com.sun.xml.bind.v2.schemagen.xmlschema.LocalElement",
            "com.sun.xml.bind.v2.schemagen.xmlschema.NestedParticle",
            "com.sun.xml.bind.v2.schemagen.xmlschema.NoFixedFacet",
            "com.sun.xml.bind.v2.schemagen.xmlschema.Occurs",
            "com.sun.xml.bind.v2.schemagen.xmlschema.Particle",
            "com.sun.xml.bind.v2.schemagen.xmlschema.Redefinable",
            "com.sun.xml.bind.v2.schemagen.xmlschema.Schema",
            "com.sun.xml.bind.v2.schemagen.xmlschema.SchemaTop",
            "com.sun.xml.bind.v2.schemagen.xmlschema.SimpleContent",
            "com.sun.xml.bind.v2.schemagen.xmlschema.SimpleDerivation",
            "com.sun.xml.bind.v2.schemagen.xmlschema.SimpleExtension",
            "com.sun.xml.bind.v2.schemagen.xmlschema.SimpleRestriction",
            "com.sun.xml.bind.v2.schemagen.xmlschema.SimpleRestrictionModel",
            "com.sun.xml.bind.v2.schemagen.xmlschema.SimpleType",
            "com.sun.xml.bind.v2.schemagen.xmlschema.SimpleTypeHost",
            "com.sun.xml.bind.v2.schemagen.xmlschema.TopLevelAttribute",
            "com.sun.xml.bind.v2.schemagen.xmlschema.TopLevelElement",
            "com.sun.xml.bind.v2.schemagen.xmlschema.TypeDefParticle",
            "com.sun.xml.bind.v2.schemagen.xmlschema.TypeHost",
            "com.sun.xml.bind.v2.schemagen.xmlschema.Union",
            "com.sun.xml.bind.v2.schemagen.xmlschema.Wildcard",
            "com.sun.xml.txw2.TypedXmlWriter");

    @BuildStep
    void processAnnotationsAndIndexFiles(
            BuildProducer<NativeImageSystemPropertyBuildItem> nativeImageProps,
            BuildProducer<ServiceProviderBuildItem> providerItem,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxyDefinitions,
            CombinedIndexBuildItem combinedIndexBuildItem,
            List<JaxbFileRootBuildItem> fileRoots,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<NativeImageResourceBundleBuildItem> resourceBundle,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeClasses,
            BuildProducer<JaxbClassesToBeBoundBuildItem> classesToBeBoundProducer,
            ApplicationArchivesBuildItem applicationArchivesBuildItem) throws ClassNotFoundException {

        List<String> classesToBeBound = new ArrayList<>();
        IndexView index = combinedIndexBuildItem.getIndex();

        // Register classes for reflection based on JAXB annotations
        boolean jaxbRootAnnotationsDetected = false;

        for (DotName jaxbRootAnnotation : JAXB_ROOT_ANNOTATIONS) {
            for (AnnotationInstance jaxbRootAnnotationInstance : index
                    .getAnnotations(jaxbRootAnnotation)) {
                if (jaxbRootAnnotationInstance.target().kind() == Kind.CLASS) {
                    String className = jaxbRootAnnotationInstance.target().asClass().name().toString();
                    reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, className));
                    classesToBeBound.add(className);
                    jaxbRootAnnotationsDetected = true;
                }
            }
        }

        if (!jaxbRootAnnotationsDetected && fileRoots.isEmpty()) {
            return;
        }

        // Register package-infos for reflection
        for (AnnotationInstance xmlSchemaInstance : index.getAnnotations(XML_SCHEMA)) {
            if (xmlSchemaInstance.target().kind() == Kind.CLASS) {
                String className = xmlSchemaInstance.target().asClass().name().toString();

                reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, className));
            }
        }

        // Register XML Java type adapters for reflection
        for (AnnotationInstance xmlJavaTypeAdapterInstance : index.getAnnotations(XML_JAVA_TYPE_ADAPTER)) {
            reflectiveClass.produce(
                    new ReflectiveClassBuildItem(true, true, xmlJavaTypeAdapterInstance.value().asClass().name().toString()));
        }

        if (!index.getAnnotations(XML_ANY_ELEMENT).isEmpty()) {
            addReflectiveClass(reflectiveClass, false, false, "javax.xml.bind.annotation.W3CDomHandler");
        }

        JAXB_ANNOTATIONS.stream()
                .map(Class::getName)
                .forEach(className -> {
                    addReflectiveClass(reflectiveClass, true, false, className);
                });

        // Register @XmlSeeAlso
        proxyDefinitions.produce(new NativeImageProxyDefinitionBuildItem(XmlSeeAlso.class.getName(),
                "com.sun.xml.bind.v2.model.annotation.Locatable"));
        for (AnnotationInstance xmlSeeAlsoAnn : index.getAnnotations(XML_SEE_ALSO)) {
            AnnotationValue value = xmlSeeAlsoAnn.value();
            Type[] types = value.asClassArray();
            for (Type t : types) {
                addReflectiveClass(reflectiveClass, false, false, t.name().toString());
            }
        }
        // Register Native proxy definitions
        for (String s : NATIVE_PROXY_DEFINITIONS) {
            proxyDefinitions.produce(new NativeImageProxyDefinitionBuildItem(s));
        }

        for (JaxbFileRootBuildItem i : fileRoots) {
            iterateResources(applicationArchivesBuildItem, i.getFileRoot(), resource, reflectiveClass, classesToBeBound);
        }

        classesToBeBoundProducer.produce(new JaxbClassesToBeBoundBuildItem(classesToBeBound));
    }

    @BuildStep
    void ignoreWarnings(BuildProducer<ReflectiveHierarchyIgnoreWarningBuildItem> ignoreWarningProducer) {
        for (DotName type : IGNORE_TYPES) {
            ignoreWarningProducer.produce(new ReflectiveHierarchyIgnoreWarningBuildItem(type));
        }
    }

    @BuildStep
    void registerClasses(
            BuildProducer<NativeImageSystemPropertyBuildItem> nativeImageProps,
            BuildProducer<ServiceProviderBuildItem> providerItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourceBundleBuildItem> resourceBundle) {
        addReflectiveClass(reflectiveClass, true, false, "com.sun.xml.bind.v2.ContextFactory");
        addReflectiveClass(reflectiveClass, true, false, "com.sun.xml.internal.stream.XMLInputFactoryImpl");
        addReflectiveClass(reflectiveClass, true, false, "com.sun.xml.internal.stream.XMLOutputFactoryImpl");
        addReflectiveClass(reflectiveClass, true, false, "com.sun.org.apache.xpath.internal.functions.FuncNot");
        addReflectiveClass(reflectiveClass, true, false, "com.sun.org.apache.xerces.internal.impl.dv.xs.SchemaDVFactoryImpl");

        addResourceBundle(resourceBundle, "javax.xml.bind.Messages");
        addResourceBundle(resourceBundle, "javax.xml.bind.helpers.Messages");

        nativeImageProps
                .produce(new NativeImageSystemPropertyBuildItem("com.sun.xml.bind.v2.bytecode.ClassTailor.noOptimize", "true"));

        JAXB_REFLECTIVE_CLASSES.stream()
                .map(Class::getName)
                .forEach(className -> addReflectiveClass(reflectiveClass, true, false, className));

        providerItem
                .produce(new ServiceProviderBuildItem(JAXBContext.class.getName(), "com.sun.xml.bind.v2.ContextFactory"));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void setupJaxbContextConfig(List<JaxbClassesToBeBoundBuildItem> classesToBeBoundBuildItems,
            JaxbContextConfigRecorder jaxbContextConfig) {
        for (JaxbClassesToBeBoundBuildItem classesToBeBoundBuildItem : classesToBeBoundBuildItems) {
            jaxbContextConfig.addClassesToBeBound(classesToBeBoundBuildItem.getClasses());
        }
    }

    @BuildStep
    void registerProduces(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(new AdditionalBeanBuildItem(JaxbContextProducer.class));
    }

    private void handleJaxbFile(Path p, BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            List<String> classesToBeBound) {
        try {
            String path = p.toAbsolutePath().toString().substring(1);
            String pkg = p.toAbsolutePath().getParent().toString().substring(1)
                    .replace(p.getFileSystem().getSeparator(), ".") + ".";

            resource.produce(new NativeImageResourceBuildItem(path));

            for (String line : Files.readAllLines(p)) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    String clazz = pkg + line;
                    Class<?> cl = Class.forName(clazz, false, Thread.currentThread().getContextClassLoader());
                    classesToBeBound.add(clazz);

                    while (cl != Object.class) {
                        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, cl));
                        cl = cl.getSuperclass();
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void iterateResources(ApplicationArchivesBuildItem applicationArchivesBuildItem, String path,
            BuildProducer<NativeImageResourceBuildItem> resource, BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            List<String> classesToBeBound) {
        for (ApplicationArchive archive : applicationArchivesBuildItem.getAllApplicationArchives()) {
            archive.accept(tree -> {
                var arch = tree.getPath(path);
                if (arch != null && Files.isDirectory(arch)) {
                    JaxbProcessor.safeWalk(arch)
                            .filter(Files::isRegularFile)
                            .filter(p -> p.getFileName().toString().equals("jaxb.index"))
                            .forEach(p1 -> handleJaxbFile(p1, resource, reflectiveClass, classesToBeBound));
                }
            });
        }
    }

    public static Stream<Path> safeWalk(Path p) {
        try {
            return Files.walk(p);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    private void addReflectiveClass(BuildProducer<ReflectiveClassBuildItem> reflectiveClass, boolean methods, boolean fields,
            String... className) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(methods, fields, className));
    }

    private void addResourceBundle(BuildProducer<NativeImageResourceBundleBuildItem> resourceBundle, String bundle) {
        resourceBundle.produce(new NativeImageResourceBundleBuildItem(bundle));
    }
}
