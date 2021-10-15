package io.quarkus.jaxb.deployment;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
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

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
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
import io.quarkus.jaxb.runtime.JaxbConfig;

class JaxbProcessor {

    private static final List<Class<? extends Annotation>> JAXB_ANNOTATIONS = Arrays.asList(
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

    private static final List<Class<?>> JAXB_REFLECTIVE_CLASSES = Collections.singletonList(XmlAccessOrder.class);

    private static final DotName XML_ROOT_ELEMENT = DotName.createSimple(XmlRootElement.class.getName());
    private static final DotName XML_TYPE = DotName.createSimple(XmlType.class.getName());
    private static final DotName XML_REGISTRY = DotName.createSimple(XmlRegistry.class.getName());
    private static final DotName XML_SCHEMA = DotName.createSimple(XmlSchema.class.getName());
    private static final DotName XML_JAVA_TYPE_ADAPTER = DotName.createSimple(XmlJavaTypeAdapter.class.getName());
    private static final DotName XML_ANY_ELEMENT = DotName.createSimple(XmlAnyElement.class.getName());
    private static final DotName XML_SEE_ALSO = DotName.createSimple(XmlSeeAlso.class.getName());

    private static final List<DotName> JAXB_ROOT_ANNOTATIONS = Arrays.asList(XML_ROOT_ELEMENT, XML_TYPE, XML_REGISTRY);

    private static final List<DotName> IGNORE_TYPES = Collections
            .singletonList(DotName.createSimple("javax.xml.datatype.XMLGregorianCalendar"));

    @BuildStep
    void processAnnotationsAndIndexFiles(
            BuildProducer<NativeImageSystemPropertyBuildItem> nativeImageProps,
            BuildProducer<ServiceProviderBuildItem> providerItem,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxyDefinitions,
            CombinedIndexBuildItem combinedIndexBuildItem,
            JaxbConfig jaxbConfig,
            List<JaxbFileRootBuildItem> fileRoots,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<NativeImageResourceBundleBuildItem> resourceBundle,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeClasses,
            ApplicationArchivesBuildItem applicationArchivesBuildItem) {

        IndexView index = combinedIndexBuildItem.getIndex();

        // Register classes for reflection based on JAXB annotations
        boolean jaxbRootAnnotationsDetected = false;

        for (DotName jaxbRootAnnotation : JAXB_ROOT_ANNOTATIONS) {
            for (AnnotationInstance jaxbRootAnnotationInstance : index
                    .getAnnotations(jaxbRootAnnotation)) {
                if (jaxbRootAnnotationInstance.target().kind() == Kind.CLASS) {
                    addReflectiveClass(reflectiveClass, true, true,
                            jaxbRootAnnotationInstance.target().asClass().name().toString());
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
                reflectiveClass.produce(
                        new ReflectiveClassBuildItem(false, false, xmlSchemaInstance.target().asClass().name().toString()));
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
                    proxyDefinitions.produce(new NativeImageProxyDefinitionBuildItem(className,
                            "com.sun.xml.bind.v2.model.annotation.Locatable"));
                    addReflectiveClass(reflectiveClass, true, false, className);
                });

        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.marshaller.CharacterEscapeHandler"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.txw2.output.CharacterEscapeHandler"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.episode.Bindings"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.episode.SchemaBindings"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.episode.Klass"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.episode.Package"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.Annotated"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.Annotation"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.Any"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.Appinfo"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.AttrDecls"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.AttributeType"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.ComplexContent"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.ComplexExtension"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.ComplexRestriction"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.ComplexType"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.ComplexTypeHost"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.ComplexTypeModel"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem(
                        "com.sun.xml.bind.v2.schemagen.xmlschema.ContentModelContainer"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.Documentation"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.Element"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.ExplicitGroup"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.ExtensionType"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.FixedOrDefault"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.Import"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.List"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.LocalAttribute"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.LocalElement"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.NestedParticle"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.NoFixedFacet"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.Occurs"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.Particle"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.Redefinable"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.Schema"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.SchemaTop"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.SimpleContent"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.SimpleDerivation"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.SimpleExtension"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.SimpleRestriction"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem(
                        "com.sun.xml.bind.v2.schemagen.xmlschema.SimpleRestrictionModel"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.SimpleType"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.SimpleTypeHost"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.TopLevelAttribute"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.TopLevelElement"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.TypeDefParticle"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.TypeHost"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.Union"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.bind.v2.schemagen.xmlschema.Wildcard"));
        proxyDefinitions
                .produce(new NativeImageProxyDefinitionBuildItem("com.sun.xml.txw2.TypedXmlWriter"));

        List<JaxbFileRootBuildItem> jaxbFileBuildItemFromConfig = new ArrayList<>();
        if (jaxbConfig.indexPaths.isPresent()) {
            jaxbFileBuildItemFromConfig = jaxbConfig.indexPaths.get().stream()
                    .map(JaxbFileRootBuildItem::new).collect(Collectors.toList());
        }

        List<JaxbFileRootBuildItem> allJaxbFileBuildItens = Stream
                .concat(jaxbFileBuildItemFromConfig.stream(), fileRoots.stream())
                .collect(Collectors.toList());

        for (JaxbFileRootBuildItem i : allJaxbFileBuildItens) {
            try (Stream<Path> stream = iterateResources(applicationArchivesBuildItem, i.getFileRoot())) {
                stream.filter(p -> p.getFileName().toString().equals("jaxb.index"))
                        .forEach(p1 -> handleJaxbFile(p1, resource, reflectiveClass));
            }
        }
    }

    @BuildStep
    void seeAlso(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveItems) {
        IndexView index = combinedIndexBuildItem.getIndex();
        for (AnnotationInstance xmlSeeAlsoAnn : index.getAnnotations(XML_SEE_ALSO)) {
            AnnotationValue value = xmlSeeAlsoAnn.value();
            Type[] types = value.asClassArray();
            for (Type t : types) {
                reflectiveItems.produce(new ReflectiveClassBuildItem(false, false, t.name().toString()));
            }
        }
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
            BuildProducer<ServiceProviderBuildItem> providerItem, final BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            final BuildProducer<NativeImageResourceBundleBuildItem> resourceBundle) {

        addReflectiveClass(reflectiveClass, true, false, "com.sun.xml.bind.v2.ContextFactory");
        addReflectiveClass(reflectiveClass, true, false, "com.sun.xml.internal.bind.v2.ContextFactory");

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

    private void handleJaxbFile(Path p, BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        try {

            String path = p.toAbsolutePath().toString().substring(1);
            String pkg = p.toAbsolutePath().getParent().toString().substring(1)
                    .replace(File.separator, ".") + ".";

            resource.produce(new NativeImageResourceBuildItem(path));

            for (String line : Files.readAllLines(p)) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    String clazz = pkg + line;

                    System.out.println("class_dest: " + clazz);
                    Class<?> cl = Class.forName(clazz, false, Thread.currentThread().getContextClassLoader());

                    while (cl != Object.class) {
                        addReflectiveClass(reflectiveClass, true, true, cl.getName());
                        cl = cl.getSuperclass();
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Stream<Path> iterateResources(ApplicationArchivesBuildItem applicationArchivesBuildItem, String path) {
        return applicationArchivesBuildItem.getAllApplicationArchives().stream()
                .map(arch -> arch.getChildPath(path))
                .filter(p -> p != null && Files.isDirectory(p))
                .flatMap(JaxbProcessor::safeWalk)
                .filter(Files::isRegularFile);
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
