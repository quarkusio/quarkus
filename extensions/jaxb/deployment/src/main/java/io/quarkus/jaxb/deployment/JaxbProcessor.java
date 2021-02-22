package io.quarkus.jaxb.deployment;

import java.io.IOError;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;
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
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import com.sun.xml.bind.v2.model.annotation.Locatable;

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

    private static final List<DotName> JAXB_ROOT_ANNOTATIONS = Arrays.asList(XML_ROOT_ELEMENT, XML_TYPE, XML_REGISTRY);

    private static final List<DotName> IGNORE_TYPES = Collections
            .singletonList(DotName.createSimple("javax.xml.datatype.XMLGregorianCalendar"));

    @Inject
    ApplicationArchivesBuildItem applicationArchivesBuildItem;

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
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeClasses) {

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
                    proxyDefinitions.produce(new NativeImageProxyDefinitionBuildItem(className, Locatable.class.getName()));
                    addReflectiveClass(reflectiveClass, true, false, className);
                });

        for (JaxbFileRootBuildItem i : fileRoots) {
            try (Stream<Path> stream = iterateResources(i.getFileRoot())) {
                stream.filter(p -> p.getFileName().toString().equals("jaxb.index"))
                        .forEach(p1 -> handleJaxbFile(p1, resource, reflectiveClass));
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
            String pkg = p.toAbsolutePath().getParent().toString().substring(1).replace("/", ".") + ".";

            resource.produce(new NativeImageResourceBuildItem(path));

            for (String line : Files.readAllLines(p)) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    String clazz = pkg + line;
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

    private Stream<Path> iterateResources(String path) {
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
