package io.quarkus.jaxb.deployment;

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
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
import org.jboss.jandex.DotName;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ServiceProviderBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBundleBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateSystemPropertyBuildItem;

class JaxbProcessor {
    private static final List<Class<?>> JAXB_REFLECTIVE_CLASSES = Arrays.asList(
            XmlAccessOrder.class,
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

    private static final List<String> JAXB_SERIALIZERS = Arrays.asList(
            "html",
            "text",
            "xml",
            "unknown");
    private static final DotName XML_ROOT = DotName.createSimple("javax.xml.bind.annotation.XmlRootElement");

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;
    @Inject
    BuildProducer<SubstrateResourceBuildItem> resource;
    @Inject
    BuildProducer<SubstrateResourceBundleBuildItem> resourceBundle;
    @Inject
    BuildProducer<RuntimeInitializedClassBuildItem> runtimeClasses;
    @Inject
    ApplicationArchivesBuildItem applicationArchivesBuildItem;

    @BuildStep
    void process(BuildProducer<SubstrateSystemPropertyBuildItem> substrateProps,
            BuildProducer<ServiceProviderBuildItem> providerItem,
            CombinedIndexBuildItem combinedIndexBuildItem,
            List<JaxbFileRootBuildItem> fileRoots,
            List<JaxbEnabledBuildItem> enabled) {

        if (enabled.isEmpty()) {
            return;
        }

        Collection<AnnotationInstance> xmlRoot = combinedIndexBuildItem.getIndex().getAnnotations(XML_ROOT);
        for (AnnotationInstance i : xmlRoot) {
            addReflectiveClass(false, true, i.target().asClass().name().toString());
        }
        if (xmlRoot.isEmpty() &&
                fileRoots.isEmpty()) {
            return;
        }

        addReflectiveClass(false, false, "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
        addReflectiveClass(false, false, "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
        addReflectiveClass(false, false, "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");
        addReflectiveClass(true, false, "com.sun.xml.bind.v2.ContextFactory");
        addReflectiveClass(true, false, "com.sun.xml.internal.bind.v2.ContextFactory");

        addResourceBundle("javax.xml.bind.Messages");
        addResourceBundle("javax.xml.bind.helpers.Messages");
        addResourceBundle("com.sun.org.apache.xml.internal.serializer.utils.SerializerMessages");
        addResourceBundle("com.sun.org.apache.xml.internal.res.XMLErrorResources");
        substrateProps
                .produce(new SubstrateSystemPropertyBuildItem("com.sun.xml.bind.v2.bytecode.ClassTailor.noOptimize", "true"));

        JAXB_REFLECTIVE_CLASSES.stream()
                .map(Class::getName)
                .forEach(clazz -> addReflectiveClass(true, false, clazz));

        JAXB_SERIALIZERS.stream()
                .map(s -> "com/sun/org/apache/xml/internal/serializer/output_" + s + ".properties")
                .forEach(this::addResource);

        for (JaxbFileRootBuildItem i : fileRoots) {
            iterateResources(i.getFileRoot())
                    .filter(p -> p.getFileName().toString().equals("jaxb.index"))
                    .forEach(this::handleJaxbFile);
        }

        providerItem.produce(new ServiceProviderBuildItem(JAXBContext.class.getName(), "com.sun.xml.bind.v2.ContextFactory"));
    }

    private void handleJaxbFile(Path p) {
        try {
            String path = p.toAbsolutePath().toString().substring(1);
            String pkg = p.toAbsolutePath().getParent().toString().substring(1).replace("/", ".") + ".";

            addResource(path);

            for (String line : Files.readAllLines(p)) {
                if (!line.startsWith("#")) {
                    String clazz = pkg + line.trim();
                    Class<?> cl = Class.forName(clazz);

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

    private Stream<Path> iterateResources(String path) {
        return applicationArchivesBuildItem.getAllApplicationArchives().stream()
                .map(arch -> arch.getArchiveRoot().resolve(path))
                .filter(Files::isDirectory)
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

    private void addResource(String r) {
        resource.produce(new SubstrateResourceBuildItem(r));
    }

    private void addReflectiveClass(boolean methods, boolean fields, String... className) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(methods, fields, className));
    }

    private void addResourceBundle(String bundle) {
        resourceBundle.produce(new SubstrateResourceBundleBuildItem(bundle));
    }
}
